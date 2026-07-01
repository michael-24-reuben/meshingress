# Vendored reference: video-loop-finder

Keep the upstream repository here, not at project root:

```txt
Meshingress/vendor/media/video-loop-finder/
```

Expected script entrypoint:

```txt
Meshingress/vendor/media/video-loop-finder/video_loop_finder.py
```

Recommended install command from the Meshingress root:

```bash
git submodule add https://github.com/bbc/video-loop-finder.git Meshingress/vendor/media/video-loop-finder
```

Alternative without submodules:

```bash
git clone https://github.com/bbc/video-loop-finder.git Meshingress/vendor/media/video-loop-finder
```

Do not flatten upstream files into `Meshingress/vendor/`.
