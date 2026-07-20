# PLAN — freemind-web

**Date:** 2026-07-19 · **Status:** Draft — decisions marked ⚖ are Bobby's.
Serves `VISION.md`; read that first.

## Where we start

`master` is the pristine upstream tip (`d8c8fe2c`, identical to SourceForge
HEAD). `web` carries the inherited web module, which is a non-functional
scaffold: the frontend crashes at startup on element-id mismatches, client
and server disagree on message types, two WebSocket servers compete, and
`/api/save` writes nothing. The full file:line defect assessment lives in the
companion analysis project (`~/Projects/freemind-code/HLD.md` §Assessment).

## Milestones

Each independently shippable; each lands with its exit test executable.

- **M1 — Working browser relay.** Fix the five frontend defects; keep one
  WebSocket server (embedded Jetty + `MindMapWebSocket`, per DD-002) and
  delete the other; align message names client↔server from one shared
  constants source. Add the testability hooks while touching the code:
  `WebServer` class with `start(port)`/`stop()` (port 0 = ephemeral),
  instance-scoped session state, frontend connects on demand.
  *Exit test:* two browsers; drag a node in one, it moves in the other —
  scripted as a two-client WebSocket integration test.

- **M2 — Real save/open through the real serializer.** Extract headless
  `freemind-core` (per DD-003; the model classes inherit from Swing's tree
  classes, so this is a deliberate surgery — spike first). Server-side
  `MapSessionService` holds the authoritative node tree; `.mm`
  upload/download endpoints; atomic save (temp + rename).
  *Exit test:* edit in browser → save → open the same file in the desktop
  app, and a golden-file round-trip suite (load → save → reload → compare)
  over sample maps including old-format versions.

- **M3 — Tier-1 sharing: the flag.** Web saves honour the desktop lock-file
  convention; the web server watches the map file and, on external change,
  pushes a banner to browsers: *"«name» changed this map — Update? Merge?"*.
  The desktop side already prompts on file-time change — verify and keep it.
  Merge = per-node three-way against the last common save ⚖ (or start with
  Update/Keep only and add Merge later ⚖).
  *Exit test:* desktop saves while browser has the map open → browser shows
  the flag; browser saves while desktop has it open → desktop prompts.

- **M4 — Tier-2 sharing: real-time desktop bridge.** A desktop plugin joins
  `ws://host:8080/ws/mindmap` as a client; remote node operations apply
  through the ActionRegistry (undoable); local edits stream out. Off by
  default, toggled per map.
  *Exit test:* drag a node in the browser, watch it move in the running
  desktop app (and vice versa), plus an automated desktop-client integration
  test against the server.

- **M5 — Test consolidation.** Whatever M1–M4 didn't cover: protocol
  round-trip for every message type, `AppServlet` path-traversal probe,
  frontend contract lint (element ids and message names vs the source),
  one end-to-end happy path in CI. Open a `TESTLOG.md` here at first run.

## Tracking

| Item | Status | Date | Notes |
|---|---|---|---|
| Clean repo: upstream master + web branch on GitHub | Done | 2026-07-19 | old history archived in freemind-code |
| VISION.md / PLAN.md / CLAUDE.md | Done | 2026-07-19 | this commit |
| M1 relay | Done | 2026-07-19 | exit test `WebSocketRelayTest` (5 tests) green |
| M2 core + save | Not started | | spike the Swing-decoupling first |
| M3 flag sharing | Not started | | merge scope ⚖ |
| M4 real-time bridge | Not started | | |
| M5 test consolidation | Not started | | |
