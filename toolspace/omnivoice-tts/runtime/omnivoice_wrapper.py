#!/usr/bin/env python3
"""JSON stdin/stdout wrapper for k2-fsa/OmniVoice.

This script is intentionally boring: request JSON comes from stdin or a file,
machine JSON goes to stdout, and human logs/errors go to stderr.
"""

from __future__ import annotations

import argparse
import importlib.util
import json
import os
import sys
import time
from pathlib import Path
from typing import Any


DEFAULT_MODEL = "k2-fsa/OmniVoice"
DEFAULT_AUDIO_TOKENIZER = "eustlb/higgs-audio-v2-tokenizer"
DEFAULT_ASR_MODEL = "openai/whisper-large-v3-turbo"
SAMPLE_RATE = 24000


def main() -> int:
    parser = argparse.ArgumentParser(description="OmniVoice JSON wrapper")
    parser.add_argument("--request", help="Path to a JSON request file")
    parser.add_argument("--stdin", action="store_true", help="Read JSON request from stdin")
    args = parser.parse_args()

    try:
        request = read_request(args)
        response = dispatch(request)
        print(json.dumps(response, ensure_ascii=False), flush=True)
        return 0 if response.get("status") == "ok" else 1
    except Exception as exc:  # keep stdout JSON-only even on unexpected failures
        operation = "unknown"
        try:
            operation = request.get("operation", "unknown")  # type: ignore[name-defined]
        except Exception:
            pass
        print(str(exc), file=sys.stderr, flush=True)
        print(json.dumps(error(operation, "WRAPPER_FAILED", str(exc))), flush=True)
        return 1


def read_request(args: argparse.Namespace) -> dict[str, Any]:
    if args.request:
        raw = Path(args.request).read_text(encoding="utf-8")
    else:
        raw = sys.stdin.read()
    if not raw.strip():
        raise ValueError("request JSON is required")
    value = json.loads(raw)
    if not isinstance(value, dict):
        raise ValueError("request JSON must be an object")
    return value


def dispatch(request: dict[str, Any]) -> dict[str, Any]:
    operation = required_str(request, "operation")
    if operation == "runtime.health":
        return runtime_health(operation)
    if operation == "models.status":
        return models_status(request)
    if operation == "models.pull":
        return models_pull(request)
    if operation in {"generate", "design", "clone"}:
        return synthesize(request)
    if operation == "voices.download":
        return error(operation, "PROVIDER_NOT_IMPLEMENTED", "No provider adapters are implemented in the default wrapper.")
    return error(operation, "UNKNOWN_OPERATION", f"Unsupported operation: {operation}")


def runtime_health(operation: str) -> dict[str, Any]:
    packages = {
        "omnivoice": importlib.util.find_spec("omnivoice") is not None,
        "torch": importlib.util.find_spec("torch") is not None,
        "soundfile": importlib.util.find_spec("soundfile") is not None,
        "huggingface_hub": importlib.util.find_spec("huggingface_hub") is not None,
    }
    return {
        "status": "ok",
        "operation": operation,
        "available": packages["omnivoice"] and packages["torch"] and packages["soundfile"],
        "packages": packages,
        "model": DEFAULT_MODEL,
        "sampleRate": SAMPLE_RATE,
    }


def models_status(request: dict[str, Any]) -> dict[str, Any]:
    model = optional_str(request, "model") or DEFAULT_MODEL
    return {
        "status": "ok",
        "operation": "models.status",
        "models": [
            {
                "model": model,
                "provider": "huggingface",
                "requiredFor": "basic TTS, voice design, voice cloning",
                "optional": False,
            },
            {
                "model": DEFAULT_AUDIO_TOKENIZER,
                "provider": "huggingface",
                "requiredFor": "audio tokenization",
                "optional": False,
            },
            {
                "model": DEFAULT_ASR_MODEL,
                "provider": "huggingface",
                "requiredFor": "auto reference transcription when referenceText is missing",
                "optional": True,
            },
        ],
        "cachePath": os.environ.get("HF_HOME") or os.environ.get("HUGGINGFACE_HUB_CACHE") or "huggingface default cache",
    }


def models_pull(request: dict[str, Any]) -> dict[str, Any]:
    try:
        from huggingface_hub import snapshot_download
    except Exception as exc:
        return error("models.pull", "HUGGINGFACE_HUB_MISSING", f"huggingface_hub is not installed: {exc}")

    pulled: list[dict[str, str]] = []
    model = optional_str(request, "model") or DEFAULT_MODEL
    invalid_model = validate_model_identifier("models.pull", model)
    if invalid_model:
        return invalid_model
    pulled.append({"model": model, "path": snapshot_download(model)})
    tokenizer = optional_str(request, "audioTokenizerModel") or DEFAULT_AUDIO_TOKENIZER
    pulled.append({"model": tokenizer, "path": snapshot_download(tokenizer)})
    if bool(request.get("includeAsrModel")):
        asr_model = optional_str(request, "asrModel") or DEFAULT_ASR_MODEL
        pulled.append({"model": asr_model, "path": snapshot_download(asr_model)})
    return {"status": "ok", "operation": "models.pull", "pulled": pulled}


def synthesize(request: dict[str, Any]) -> dict[str, Any]:
    operation = required_str(request, "operation")
    text = required_str(request, "text")
    output_path = Path(required_str(request, "outputPath")).expanduser().resolve()
    model_id = optional_str(request, "model") or DEFAULT_MODEL
    invalid_model = validate_model_identifier(operation, model_id)
    if invalid_model:
        return invalid_model

    try:
        import soundfile as sf
        import torch
        from omnivoice import OmniVoice
    except Exception as exc:
        return error(operation, "OMNIVOICE_RUNTIME_MISSING", f"Install OmniVoice runtime packages first: {exc}")

    kwargs: dict[str, Any] = {}
    put_if_present(kwargs, "language_id", request.get("languageId"))
    put_if_present(kwargs, "duration", request.get("duration"))
    put_if_present(kwargs, "speed", request.get("speed"))
    put_if_present(kwargs, "num_step", request.get("numStep"))

    if operation == "design":
        kwargs["instruct"] = required_str(request, "instruct")
    elif operation == "clone":
        reference_audio = Path(required_str(request, "referenceAudioPath")).expanduser().resolve()
        if not reference_audio.is_file():
            return error(operation, "REFERENCE_AUDIO_MISSING", "Reference audio path does not exist.", {"referenceAudioPath": str(reference_audio)})
        reference_text = optional_str(request, "referenceText")
        if not reference_text and not bool(request.get("allowAutoTranscribe")):
            return error(operation, "REFERENCE_TEXT_REQUIRED", "referenceText is required unless allowAutoTranscribe is true.")
        kwargs["ref_audio"] = str(reference_audio)
        if reference_text:
            kwargs["ref_text"] = reference_text

    device = optional_str(request, "device")
    dtype = dtype_from_request(torch, optional_str(request, "dtype"))
    load_kwargs: dict[str, Any] = {}
    if device:
        load_kwargs["device_map"] = device
    if dtype is not None:
        load_kwargs["dtype"] = dtype

    start = time.monotonic()
    print(f"Loading OmniVoice model {model_id}", file=sys.stderr, flush=True)
    model = OmniVoice.from_pretrained(model_id, **load_kwargs)
    audio = model.generate(text=text, **kwargs)
    output_path.parent.mkdir(parents=True, exist_ok=True)
    sf.write(str(output_path), audio[0], getattr(model, "sampling_rate", SAMPLE_RATE))
    duration_ms = int((time.monotonic() - start) * 1000)

    return {
        "status": "ok",
        "operation": operation,
        "model": model_id,
        "outputPath": str(output_path),
        "sampleRate": getattr(model, "sampling_rate", SAMPLE_RATE),
        "durationMs": duration_ms,
        "warnings": [],
    }


def dtype_from_request(torch_module: Any, value: str | None) -> Any:
    if not value:
        return None
    normalized = value.lower().replace("torch.", "")
    mapping = {
        "float16": torch_module.float16,
        "fp16": torch_module.float16,
        "bfloat16": torch_module.bfloat16,
        "bf16": torch_module.bfloat16,
        "float32": torch_module.float32,
        "fp32": torch_module.float32,
    }
    if normalized not in mapping:
        raise ValueError(f"Unsupported dtype: {value}")
    return mapping[normalized]


def required_str(request: dict[str, Any], key: str) -> str:
    value = request.get(key)
    if not isinstance(value, str) or not value.strip():
        raise ValueError(f"{key} is required")
    return value.strip()


def optional_str(request: dict[str, Any], key: str) -> str | None:
    value = request.get(key)
    if isinstance(value, str) and value.strip():
        return value.strip()
    return None


def put_if_present(target: dict[str, Any], key: str, value: Any) -> None:
    if value is not None:
        target[key] = value


def validate_model_identifier(operation: str, model_id: str) -> dict[str, Any] | None:
    model_path = Path(model_id).expanduser()
    if model_path.name.startswith("models--") and (model_path / "snapshots").is_dir():
        return cache_folder_error(operation, model_path.name, model_path)
    if model_path.exists():
        return None
    if not model_id.startswith("models--"):
        return None

    return cache_folder_error(operation, model_id, huggingface_cache_root() / model_id)


def cache_folder_error(operation: str, provided: str, cache_path: Path) -> dict[str, Any]:
    details = {
        "provided": provided,
        "useRepositoryId": cache_folder_to_repo_id(provided),
    }
    details["cachePath"] = str(cache_path)
    snapshots_dir = cache_path / "snapshots"
    if snapshots_dir.is_dir():
        snapshots = sorted(path for path in snapshots_dir.iterdir() if path.is_dir())
        if snapshots:
            details["localSnapshotPath"] = str(snapshots[-1])

    return error(
        operation,
        "INVALID_MODEL_IDENTIFIER",
        "Hugging Face cache folder names are not model ids. Use the repository id, omit model for the default, or pass a concrete snapshots/<commit> directory.",
        details,
    )


def cache_folder_to_repo_id(model_id: str) -> str:
    if model_id.startswith("models--"):
        return model_id[len("models--") :].replace("--", "/")
    return model_id


def huggingface_cache_root() -> Path:
    explicit_cache = os.environ.get("HUGGINGFACE_HUB_CACHE")
    if explicit_cache:
        return Path(explicit_cache).expanduser()
    hf_home = os.environ.get("HF_HOME")
    if hf_home:
        return Path(hf_home).expanduser() / "hub"
    return Path.home() / ".cache" / "huggingface" / "hub"


def error(operation: str, code: str, message: str, details: dict[str, Any] | None = None) -> dict[str, Any]:
    response: dict[str, Any] = {
        "status": "error",
        "operation": operation,
        "errorCode": code,
        "errorMessage": message,
    }
    if details:
        response["details"] = details
    return response


if __name__ == "__main__":
    raise SystemExit(main())
