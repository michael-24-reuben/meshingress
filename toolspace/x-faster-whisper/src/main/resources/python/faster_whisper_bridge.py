#!/usr/bin/env python3
"""JSON stdin/stdout bridge for the provisioned faster-whisper runtime.

This process never installs Python packages and is always called with local_files_only=True.
Model acquisition belongs to provisioning, not an MCP transcription call.
"""
import argparse
import json
import sys


MODEL_KEYS = {
    "name": "model_size_or_path", "device": "device", "deviceIndexes": "device_index",
    "computeType": "compute_type", "cpuThreads": "cpu_threads", "numWorkers": "num_workers",
    "downloadRoot": "download_root", "localFilesOnly": "local_files_only", "revision": "revision",
}
GENERATION_KEYS = {
    "language": "language", "task": "task", "beamSize": "beam_size", "bestOf": "best_of",
    "patience": "patience", "lengthPenalty": "length_penalty", "repetitionPenalty": "repetition_penalty",
    "noRepeatNgramSize": "no_repeat_ngram_size", "temperature": "temperature",
    "compressionRatioThreshold": "compression_ratio_threshold", "logProbThreshold": "log_prob_threshold",
    "noSpeechThreshold": "no_speech_threshold", "conditionOnPreviousText": "condition_on_previous_text",
    "promptResetOnTemperature": "prompt_reset_on_temperature", "initialPrompt": "initial_prompt",
    "prefix": "prefix", "suppressBlank": "suppress_blank", "suppressTokens": "suppress_tokens",
    "withoutTimestamps": "without_timestamps", "maxInitialTimestamp": "max_initial_timestamp",
    "wordTimestamps": "word_timestamps", "prependPunctuations": "prepend_punctuations",
    "appendPunctuations": "append_punctuations", "multilingual": "multilingual", "vadFilter": "vad_filter",
    "vadParameters": "vad_parameters", "maxNewTokens": "max_new_tokens", "chunkLength": "chunk_length",
    "clipTimestamps": "clip_timestamps", "hallucinationSilenceThreshold": "hallucination_silence_threshold",
    "hotwords": "hotwords", "languageDetectionThreshold": "language_detection_threshold",
    "languageDetectionSegments": "language_detection_segments",
}


def mapped(values, mapping):
    return {target: values[source] for source, target in mapping.items() if values.get(source) is not None}


def verify():
    import faster_whisper
    print(json.dumps({"ok": True, "fasterWhisperVersion": getattr(faster_whisper, "__version__", "unknown")}))


def transcribe(payload):
    from faster_whisper import WhisperModel

    model_values = mapped(payload.get("model", {}), MODEL_KEYS)
    model_values["local_files_only"] = True
    model = WhisperModel(**model_values)
    generation = mapped(payload.get("generation", {}), GENERATION_KEYS)
    segments, info = model.transcribe(payload["audioPath"], **generation)
    return {
        "ok": True,
        "language": getattr(info, "language", None),
        "languageProbability": getattr(info, "language_probability", None),
        "duration": getattr(info, "duration", None),
        "segments": [
            {
                "id": segment.id, "start": segment.start, "end": segment.end, "text": segment.text,
                "avgLogprob": segment.avg_logprob, "noSpeechProb": segment.no_speech_prob,
                "compressionRatio": segment.compression_ratio,
                "words": [
                    {"start": word.start, "end": word.end, "word": word.word, "probability": word.probability}
                    for word in (segment.words or [])
                ],
            }
            for segment in segments
        ],
    }


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--verify", action="store_true")
    parser.add_argument("--json-stdin", action="store_true")
    args = parser.parse_args()
    if args.verify:
        verify()
        return
    if not args.json_stdin:
        raise SystemExit("Use --verify or --json-stdin")
    try:
        print(json.dumps(transcribe(json.load(sys.stdin))))
    except Exception as error:  # The Java wrapper turns this structured error into a tool failure.
        print(json.dumps({"ok": False, "error": str(error), "exceptionType": type(error).__name__}))
        raise SystemExit(1)


if __name__ == "__main__":
    main()
