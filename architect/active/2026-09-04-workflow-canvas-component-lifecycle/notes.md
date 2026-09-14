# Notes

- A base lifecycle should coordinate local resources; it must not absorb the
  scene tracker’s role as the authoritative owner of state transitions.
- The important split is: raw data -> `init` -> parsed model; interaction ->
  tracker -> normalized snapshot; snapshot -> `start` / `update` -> render.
