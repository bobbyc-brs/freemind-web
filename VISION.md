# VISION — one FreeMind, reachable from desktop and web

**Date:** 2026-07-19 · **Owner:** Bobby Chawla

## The vision, one paragraph

One instance of FreeMind — one mind map — accessible from the desktop
application *or* from a browser, interchangeably. Ideally, a change made in
one appears in the other in real time. Failing that, it is acceptable for the
other side to raise a flag: *"«user/client X» changed this map — Update?
Merge?"* — the way FreeMind already prompts when a file changes on disk.

## What that means concretely

Three usage scenarios, all against the same `.mm` map:

1. **Desktop only** — today's FreeMind, unchanged. Nothing regresses.
2. **Web only** — a browser opens, edits, and saves the map through the web
   server. The saved file is a normal `.mm` the desktop can open later.
3. **Both at once** — the interesting case, in two tiers:
   - **Tier 1 (flag):** each side detects the other's save and prompts —
     *Update / Merge / Keep mine*. The desktop already has the machinery
     (file modification time checks, lock files); the web side gains the
     equivalent.
   - **Tier 2 (real-time):** the desktop app joins the web server's
     WebSocket protocol as just another client — edits stream both ways as
     node operations, applied through the desktop's undo framework so a
     remote change is undoable like any local one.

Tier 1 is the commitment; Tier 2 is the ideal we build toward.

## Also in scope

- **The code improvements already identified:** one WebSocket server, not
  two; a headless `freemind-core` so the 20-year-proven `.mm` serializer is
  reused rather than reimplemented; a server-side authoritative map model;
  testability hooks (ephemeral-port server start, instance-scoped session
  state, a frontend that doesn't auto-connect at script load).
- **Formal tests:** every milestone lands with its exit test executable, on a
  base of golden-file `.mm` round-trips, protocol round-trips, and two-client
  WebSocket integration tests.

## Principles

- **Never fork the file format.** The desktop serializer is the single
  source of truth for `.mm`; the web side calls it, never imitates it.
- **Nothing regresses for desktop-only users.** All bridging is additive
  (plugin / separate module), switchable off.
- **Respect the locks.** Web saves honour the desktop's lock-file
  convention so the two sides cannot silently clobber each other.
- **Conflict honesty over conflict magic.** When both sides changed the same
  thing, say so and ask — a clear prompt beats a silent merge that guesses
  wrong. Per-node last-writer-wins only where the user has opted into
  real-time mode.

## Non-goals (for now)

- Multi-map collaboration service, accounts, authentication
- CRDT/OT concurrent editing
- Mobile clients
- Serving arbitrary maps to the open internet — this is one instance for
  one user (or one trusted household/team) first
