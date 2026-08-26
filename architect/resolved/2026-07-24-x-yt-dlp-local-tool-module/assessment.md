# Assessment

The local source checkout contained the complete `yt_dlp` package and an installed `yt-dlp-ejs 0.8.0` package. Both can run from module-owned `vendor/python` with Python's `-S` switch, which excludes host site-packages. A real metadata-only probe against `https://youtu.be/2x7wq9s85sk` completed through the vendored runtime and explicit Node selector.

The module deliberately requires host Python and Node executables. It has no dependency on the external Dev.py checkout, virtual environment, package cache, or runtime-installed Python libraries.
