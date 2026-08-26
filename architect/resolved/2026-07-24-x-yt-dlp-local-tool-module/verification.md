# Verification

## Commands

```powershell
.\mvnw.cmd -pl toolspace/x-yt-dlp -am test
.\mvnw.cmd -pl app/meshingress-server -am test "-Dtest=McpYtDlpToolMvcTests" "-Dsurefire.failIfNoSpecifiedTests=false"
$env:PYTHONPATH = "toolspace/x-yt-dlp/vendor/python"
python -S -m yt_dlp --version
python -S -c "from yt_dlp_ejs import version; import yt_dlp_ejs.yt.solver; print(version)"
python -S -m yt_dlp --js-runtimes node --simulate --skip-download --dump-single-json 'https://youtu.be/2x7wq9s85sk'
```

## Result

- Focused module reactor passed, including `YtDlpBackendTests`.
- Server reactor passed; `McpYtDlpToolMvcTests` reported 1 test, 0 failures, and 0 errors.
- Vendored runtime reported yt-dlp `2026.07.04` and imported yt-dlp-ejs `0.8.0` without host site-packages.
- The metadata-only probe returned a full JSON document. No media bytes were downloaded during verification.

## Remaining limitations

- Python and Node are host executables, not bundled Windows runtimes.
- Source availability, format availability, and platform permissions remain upstream-dependent at call time.
