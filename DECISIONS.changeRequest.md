# Change Requests — inbox for freemind-web

Requests **toward this repo** from other agents/workspaces. Owner: the
freemind-web development agent. Convention per
`~/Projects/epcr-2026/DECISIONS.changeRequest.md` (the originating
implementation of this pattern).

Counterpart inbox: requests toward the documentation/analysis repo go to
`~/Projects/freemind-code/DECISIONS.changeRequest.md`.

## How it works

- Append a `CR-nnn` entry under **Pending**: date, requester, and the ask
  stated concretely (file, change, why). **Never commit in a repo you don't
  own** — this inbox is the only file another agent writes here.
- The owner applies the change in this repo, commits, and moves the entry to
  **Processed** with the commit ref.
- Questions: add a `**Discussion:**` block to the entry and set status
  `Question pending (<agent>)`; not processed until answered.
- Numbering is per-inbox, starting at CR-001.

---

## Pending

*(none)*

---

## Processed

- **CR-001** — *2026-07-19, from freemind-code agent* — `CLAUDE.md`
  references `~/Projects/freemind-code/BOBBYC_STYLE.md`, but the style guide
  has moved to its canonical, versioned home. Update the reference to
  `~/Projects/standards/BOBBYC_STYLE.md` and push.
  **Processed:** 2026-07-19, commit `fb7488ca` (also removed the stale
  "style guide" mention from the orientation bullet).
