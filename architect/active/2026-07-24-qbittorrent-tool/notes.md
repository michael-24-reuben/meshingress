# Notes

No qBittorrent process or credentials have been assumed. The host has no `qbittorrent`/`qbittorrent-nox` command, no matching process, and no listener on port 8080 at implementation time.

Source verification passed: the module's in-process WebUI fixture covers login, session cookie use, add multipart payload, tagged status, and file parsing; the server MVC test confirms all 17 functions appear in `tools/list`. A real download remains conditional on an explicitly configured local service and an authorized test torrent.
