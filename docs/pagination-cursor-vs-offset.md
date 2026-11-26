# Pagination from First Principles: Offset vs Cursor

**A tutorial for `bookstore-microservices`** — written to be read top to bottom. Every
concept is built from absolute basics, with diagrams, step-by-step timelines, and
real production examples. By the end you will understand not just *how* both pagination
styles work, but *why* cursor pagination exists at all.

---

## Table of contents

**Part 1 — The foundations**
1. [What the database gives you](#1-what-the-database-gives-you)
2. [Why we can't return everything](#2-why-we-cant-return-everything)
3. [The database's built-in answer: LIMIT and OFFSET](#3-the-databases-built-in-answer-limit-and-offset)
4. [How OFFSET really works inside the database](#4-how-offset-really-works-inside-the-database)

**Part 2 — Building offset pagination**
5. [Turning LIMIT/OFFSET into an API: page numbers](#5-turning-limitoffset-into-an-api-page-numbers)
6. [The completeness problem: how many pages are there?](#6-the-completeness-problem-how-many-pages-are-there)

**Part 3 — Where offset pagination breaks**
7. [The key insight: a result set is not a snapshot](#7-the-key-insight-a-result-set-is-not-a-snapshot)
8. [Failure mode 1: duplicates](#8-failure-mode-1-duplicates)
9. [Failure mode 2: skipped rows](#9-failure-mode-2-skipped-rows)
10. [Failure mode 3: deep pages get slower and slower](#10-failure-mode-3-deep-pages-get-slower-and-slower)
11. [Failure mode 4: the COUNT tax on every request](#11-failure-mode-4-the-count-tax-on-every-request)

**Part 4 — Building cursor pagination**
12. [The fix: stop anchoring to positions, anchor to landmarks](#12-the-fix-stop-anchoring-to-positions-anchor-to-landmarks)
13. [The cursor query](#13-the-cursor-query)
14. [What the database actually does: index seek vs scan](#14-what-the-database-actually-does-index-seek-vs-scan)
15. [The "+1 trick": knowing when to stop](#15-the-1-trick-knowing-when-to-stop)
16. [The off-by-one trap](#16-the-off-by-one-trap)
17. [Why the cursor must be opaque (and versioned)](#17-why-the-cursor-must-be-opaque-and-versioned)
18. [When the sort key is not unique: tuple comparison](#18-when-the-sort-key-is-not-unique-tuple-comparison)
19. [Going backwards: previous-page links](#19-going-backwards-previous-page-links)

**Part 5 — The real world**
20. [How production APIs do it](#20-how-production-apis-do-it)
21. [Side-by-side comparison](#21-side-by-side-comparison)
22. [Which one should you use? A decision guide](#22-which-one-should-you-use-a-decision-guide)

**Part 6 — This project**
23. [The implementation, explained line by line](#23-the-implementation-explained-line-by-line)
24. [Experiments you can run](#24-experiments-you-can-run)
25. [Exercises](#25-exercises)

---

# Part 1 — The foundations

## 1. What the database gives you

Start with the smallest possible setup. You have a table:

```sql
CREATE TABLE products (
    id     bigint PRIMARY KEY,
    code   text NOT NULL UNIQUE,
    name   text NOT NULL,
    price  numeric(10,2) NOT NULL
);
```

with 15 rows, whose codes happen to be `P101` through `P115`:

```
id  │ code  │ name              │ price
────┼───────┼───────────────────┼───────
 1  │ P101  │ The Pragmatic...  │ 42.50
 2  │ P102  │ Clean Code        │ 38.00
 3  │ P103  │ Refactoring       │ 45.00
 .. │ ...   │ ...               │ ...
15  │ P115  │ Domain-Driven...  │ 55.00
```

A plain query returns **all** rows:

```sql
SELECT * FROM products ORDER BY code;
```

The database runs off, finds every row, sorts them by `code`, and hands back 15 rows.
Simple. Understandable. And, in production, completely unusable. Why? That's the next
section — but before we get there, notice one thing about `ORDER BY code`: the database
has now put these rows in a **total order**. Row 1 is `P101`, row 2 is `P102`, … row 15
is `P115`. That ordering is what all pagination is built on. Keep it in mind.

## 2. Why we can't return everything

Imagine the catalog grows to 2 million products. What happens when a client calls
`GET /api/products`?

```
Client                          Server                       Database
  │                               │                             │
  │  GET /api/products            │                             │
  │──────────────────────────────▶│                             │
  │                               │  SELECT * FROM products     │
  │                               │────────────────────────────▶│
  │                               │                             │ reads 2M rows
  │                               │◀──── 2M rows, ~1.5 GB ──────│
  │                               │  serializes to JSON...      │
  │                               │  (another ~2 GB in memory)  │
  │◀──── 1.5 GB of JSON ─────────│                             │
  │                               │                             │
  │  mobile app: OOM crash.       │  server: GC pauses,         │
  │  browser: frozen tab.         │  thread pool exhausted,     │
  │                               │  other requests starve.     │
```

Nobody needs 2 million rows at once. A phone screen shows maybe 10. A web page shows
50. Returning everything costs the server memory, the network bandwidth, and the client
battery — to deliver data that is 99.9% thrown away.

**Pagination is the answer**: deliver the data in *chunks*, and give the client a way to
ask for the next chunk.

But notice the wording. The client needs to say two things:

1. *"Here is the chunk I want."*
2. *"Here is how I'll ask for the next one."*

Every pagination scheme ever designed is an answer to the question: **how does the client
describe where it is in the list?** Hold on to that sentence — it is the whole tutorial in
one line.

## 3. The database's built-in answer: LIMIT and OFFSET

SQL has native keywords for chunking:

```sql
SELECT * FROM products
ORDER BY code
LIMIT 10 OFFSET 0;    -- rows 1–10:   the first chunk
```

```sql
SELECT * FROM products
ORDER BY code
LIMIT 10 OFFSET 10;   -- rows 11–20:  the second chunk
```

```sql
SELECT * FROM products
ORDER BY code
LIMIT 10 OFFSET 20;   -- rows 21–30:  the third chunk
```

The meanings:

- **`LIMIT 10`** — "return at most 10 rows."
- **`OFFSET 10`** — "before returning anything, *skip* the first 10 rows of the result
  set."

So the client describes its position as a **number of rows to skip**. This is
**offset pagination**, and it is the most natural first answer — it is what almost
everyone builds first, and it is what your `ProductService.getAllProducts` does today.

## 4. How OFFSET really works inside the database

Here is a detail most developers never learn, and it explains half of this tutorial.

When you write `LIMIT 10 OFFSET 100000`, you might imagine the database "jumping" to row
100,001 like a seek. **It does not.** The database must *materialize the ordered result
set, walk past the first 100,000 rows one by one, throw them away, and only then start
returning rows*:

```
Query:  SELECT * FROM products ORDER BY code LIMIT 10 OFFSET 100000

        ┌──────────────────────────────────────────────────────────────┐
        │  sorted result set (conceptually)                            │
        │                                                              │
        │  row 1        row 2        ...   row 100000   row 100001..   │
        │  ┌─────┐     ┌─────┐            ┌────────┐    ┌──────────┐   │
        │  │P101 │     │P102 │    ...     │P101100 │    │ these 10 │   │
        │  └─────┘     └─────┘            └────────┘    │ get      │   │
        │                                               │ returned │   │
        │  └──────────── walk & discard ────────────────┘ └──────────┘   │
        │           (100,000 rows read for nothing)                    │
        └──────────────────────────────────────────────────────────────┘
```

Page 1 (offset 0) reads 10 rows. Page 10,000 reads 100,010 rows — and *discards* 100,000
of them. **The deeper you page, the more work every request does.** We will come back to
this in [§10](#10-failure-mode-3-deep-pages-get-slower-and-slower).

---

# Part 2 — Building offset pagination

## 5. Turning LIMIT/OFFSET into an API: page numbers

Rows-to-skip is a terrible API parameter. Nobody thinks "I want to skip 10 rows." They
think "I want **page 2**." So the API exposes a page number, and the server converts it:

```
API:  GET /api/v1/products?page=2

Server-side conversion (page size = 10, 1-based pages):
      page 2  →  offset (2 - 1) × 10 = 10  →  LIMIT 10 OFFSET 10
```

```
page 1 = rows 1–10        (OFFSET 0)
page 2 = rows 11–20       (OFFSET 10)
page 3 = rows 21–30       (OFFSET 20)
page N = rows (N-1)×10+1 .. N×10
```

### The 0-based vs 1-based trap

Here is a subtlety that has bitten real production systems. **Two numbering conventions
exist side by side, and they disagree by one:**

| Convention | First page is | Used by |
|---|---|---|
| **1-based** (human-friendly) | `page=1` | most public APIs, humans |
| **0-based** (computer-friendly) | `page=0` | Spring Data, MongoDB, Elasticsearch |

Your Spring Data repository is 0-based: `PageRequest.of(0, 10)` fetches the *first* page.
But a human client sending `?page=1` means *the first page*. If the server passed the
1-based number straight through to Spring Data, `page=1` would fetch the **second** page
— an off-by-one bug so common it has a name.

The fix is to translate **at the boundary**, in exactly one place:

```java
// Controller: speaks the API's language (1-based)
@GetMapping
public PageResponse<Product> getAllProducts(
        @RequestParam(value = "page", defaultValue = "1") Integer pageNo) {
    return productService.getAllProducts(pageNo);
}

// Service: converts to Spring Data's language (0-based) — the ONLY place this happens
pageNo = pageNo <= 1 ? 0 : pageNo - 1;   // page 1 → PageRequest.of(0, ...)
Pageable pageable = PageRequest.of(pageNo, PAGE_SIZE, sort);
```

**Rule: pick one convention for the API, pick one convention for the framework, and
convert in exactly one place.** If the conversion is scattered across controllers, you
will eventually get `page=1` behaving differently on two endpoints.

## 6. The completeness problem: how many pages are there?

A page number only makes sense if the client knows how many pages exist. `?page=7` is
useless if you don't know whether page 7 exists. So the offset API must also answer:
**"how much data is there in total?"**

That requires a second query:

```sql
SELECT count(*) FROM products;   -- → 15
```

From the count, the server derives everything else:

```
totalElements = 15
totalPages    = ceil(15 / 10) = 2
isFirst       = (page == 1)
isLast        = (page == totalPages)
hasNext       = (page < totalPages)
hasPrevious   = (page > 1)
```

And the response becomes a rich object:

```json
{
  "totalElements": 15,
  "totalPages": 2,
  "pageNumber": 2,
  "isFirst": false,
  "isLast": true,
  "hasNext": false,
  "hasPrevious": true,
  "data": [ ...rows 11–15... ]
}
```

This is a genuinely good API for a human: "page 2 of 2", "15 results total", clickable
page numbers. UIs love it. **But every one of those fields is derived from a *position*
in the result set — and positions are the dangerous thing.** The next part shows why.

---

# Part 3 — Where offset pagination breaks

## 7. The key insight: a result set is not a snapshot

Here is the assumption hiding inside offset pagination, stated plainly:

> *"The result set is the same list every time I query it."*

In other words: if rows 11–20 are `P111..P120` right now, then when I ask for rows 21–30
a moment later, the list is still the same list, and row 21 is still `P121`. The rows are
numbered by their **position in the sorted result**, not by their id or their content.

**This assumption is false in any real system.** The database is shared. Other users,
other services, background jobs — they insert and delete rows *while you are paging*.

```
Time ──────────────────────────────────────────────────────────────▶

   User A is paging through the catalog:

   t0:  A requests page 1   →  rows 1–10
   t1:  A reads the page    →  (takes a few seconds — humans are slow)
   t2:  User B POSTs a new product
   t3:  A requests page 2   →  rows 11–20  ← but of WHICH list?
```

The question in the last line is the crux. There is no single list. At t0 the sorted list
had 15 rows. At t3 it has 16. Offset pagination computes positions *fresh on every
query* — "row 11" at t3 means "row 11 of the **current** table," not "row 11 of the table
A saw at t0."

The next two sections trace exactly what goes wrong, in painful detail. We will use the
15-product catalog, page size 10, and a single insert.

## 8. Failure mode 1: duplicates

Setup: the catalog has 15 products, `P101..P115`. User A is browsing, page size 10.
With 15 rows, there are two pages: page 1 holds rows 1–10, page 2 holds rows 11–15.

```
Sorted list (t0):   P101 P102 P103 P104 P105 P106 P107 P108 P109 P110 │ P111 P112 P113 P114 P115
                    └────────────── page 1 ──────────────────────────┘ └────── page 2 ──────┘
```

**Step 1 —** A requests `?page=1`. The server runs `LIMIT 10 OFFSET 0`:

```
Server returns rows 1–10:   P101 P102 P103 P104 P105 P106 P107 P108 P109 P110
                                                                              └── last row A saw: P110
```

**Step 2 —** While A is reading, another user POSTs a new product with code `P055`.
Since `P055` sorts *before* every existing code, the insert lands at the **front** of the
sorted order. Every existing row shifts down by one position:

```
Sorted list (t1):   P055 P101 P102 P103 P104 P105 P106 P107 P108 P109 P110 │ P111 P112 P113 P114 P115
                    └──────────────────── page 1 ─────────────────────────┘ └──── page 2 ─────────┘
                    ▲ inserted
```

The list now has 16 rows — still 2 pages, but the page *boundaries* have moved: page 1
now ends after `P110` (row 11), and page 2 is rows 12–16.

Look at what happened to `P110`. At t0 it was row 10 — the **last row of page 1**. At t1
it is row 11 — the **first row of page 2**. Its *position* changed, but nothing about the
row changed. Positions are not properties of rows; they are properties of the *query*.

**Step 3 —** A requests `?page=2`. The server runs `LIMIT 10 OFFSET 10` against the
*current* table:

```
Offset 10 skips rows 1–10:   P055 P101 P102 P103 P104 P105 P106 P107 P108 P109
                             └──────────────── skipped ─────────────────────────┘
Rows 11–16 returned:                                                            P110 P111 P112 P113 P114 P115
                                                                                └─ wait. P110? AGAIN?
```

`P110` was the **last row of page 1**. Now it is the **first row of page 2**. The user
sees the same product twice, and — worse — has no idea whether anything else is wrong.
The response carries no error. The `totalElements` even updates politely from 15 to 16.
Everything looks fine. The data is wrong.

```
   A's view of the catalog:

   page 1:  P101 P102 ... P109 [P110]
   page 2:  [P110] P111 P112 P113 P114 P115     ← P110 seen twice
             ▲
             └── duplicate — and there is no way to tell whether
                 any OTHER rows were also shifted into or out of
                 the pages A already walked past
```

**Real-world version:** you are on a shopping site, click "next page", and a product you
already added to your cart appears again in the listing. This is one of the most common
pagination bugs in the industry, and it is invisible in development — because *you* are
the only user, and nothing changes the table while you page.

## 9. Failure mode 2: skipped rows

Deletions produce the mirror-image bug. Setup: same 15 products, same walk.

**Step 1 —** A requests `?page=1` → rows 1–10: `P101..P110`.

**Step 2 —** An admin deletes `P101` (a discontinued product). Every row after it shifts
**up** by one position:

```
Sorted list (t0):   P101 P102 P103 P104 P105 P106 P107 P108 P109 P110 │ P111 P112 P113 P114 P115
                    └──────────────── page 1 ─────────────────────────┘ └────── page 2 ──────┘

Sorted list (t1):   P102 P103 P104 P105 P106 P107 P108 P109 P110 P111 │ P112 P113 P114 P115
                    └──────────────── page 1 ─────────────────────────┘ └── page 2 ─────────┘
                    ▲ deleted
```

The list now has 14 rows. `P111`, which was row 11 at t0, is now row 10 — it moved
**into page 1** without the user ever seeing it there.

**Step 3 —** A requests `?page=2`. The server runs `LIMIT 10 OFFSET 10` against the
current table:

```
Offset 10 skips rows 1–10:   P102 P103 P104 P105 P106 P107 P108 P109 P110 P111
                             └─────────────────── skipped ──────────────────────┘
Rows 11–14 returned:                                                              P112 P113 P114 P115
```

`P111` was **never returned on any page**. It wasn't on page 1 (A's walk had ended at
row 10, and P111 was row 11 then), and it isn't on page 2 (it's row 10 now — skipped).
The user silently misses it. On a shopping site, that is a *lost sale*. In an audit log,
it is a *compliance incident*.

```
   A's view:

   page 1:  P101 P102 ... P109 P110
   page 2:  P112 P113 P114 P115              ← P111 is in NEITHER page
```

Notice the symmetry: **inserts shift rows down (→ duplicates); deletes shift rows up (→
skipped rows).** Both bugs come from the same root cause — the anchor is a *position*,
and positions move when the list changes.

## 10. Failure mode 3: deep pages get slower and slower

From [§4](#4-how-offset-really-works-inside-the-database): `OFFSET N` forces the database
to walk and discard N rows on every request. Let's see it as a cost table. Assume the
database reads ~1M rows/sec (optimistic for a wide table).

| Page requested | Rows read | Rows discarded | Time |
|---|---|---|---|
| 1 | 10 | 0 | ~1 ms |
| 100 | 1,000 | 990 | ~1 ms |
| 1,000 | 10,000 | 9,990 | ~10 ms |
| 10,000 | 100,000 | 99,990 | ~100 ms |
| 100,000 | 1,000,000 | 999,990 | ~1 s |
| 1,000,000 | 10,000,000 | 9,999,990 | ~10 s |

The user did nothing wrong — they just kept clicking "next". Meanwhile every page load
gets slower, and row 10,000,000 — if anyone ever reaches it — takes ten seconds.

Worse: this is a **denial-of-service vector**. A single request — `?page=999999999` —
makes the database do enormous work for ten rows. No authentication, no rate limiting, no
special privileges needed. Anyone with a browser can do it.

## 11. Failure mode 4: the COUNT tax on every request

Remember [§6](#6-the-completeness-problem-how-many-pages-are-there): every offset page
response includes `totalElements` and `totalPages`. To fill them, Spring Data runs a
second query on **every** page request:

```sql
SELECT count(*) FROM products;
```

`count(*)` in PostgreSQL scans the table (or an index) and counts rows. On a 2-million-row
table that might take 100–300 ms — **paid on every single page view**, just to display
"page 3 of 200,000". And the number is stale the moment it's displayed.

---

# Part 4 — Building cursor pagination

## 12. The fix: stop anchoring to positions, anchor to landmarks

Let's re-read the question from [§2](#2-why-we-cant-return-everything): *how does the
client describe where it is in the list?*

Offset's answer: *"at row 11"* — a **position**. And we just proved positions are
unstable: `P110` was row 10, then row 11, without moving. The position is not attached to
the row; it's attached to the *state of the table at query time*.

So the fix is to anchor to something that **is** attached to the row. What is?

```
Sorted list (t0):   P101 P102 P103 ... P109 P110
                                    └──────────┘
                                    these rows have a property that
                                    does NOT change when other rows
                                    are inserted or deleted:

                                    their own value of `code`.
```

**`P110`'s code is `P110` no matter how many rows get inserted or deleted around it.** Its
position changes; its value doesn't. So instead of saying:

> "give me rows 11–16" *(position — unstable)*

the client says:

> "give me the rows that come **after** the row whose code is `P110`" *(landmark — stable)*

That reference — *"the last thing I saw"* — is called a **cursor**. The name comes from
your text editor's cursor: it marks a position *in the content*, not in a numbered list.
Delete text before the cursor and the cursor stays put. That is precisely the property we
want.

```
   OFFSET:                CURSOR:

   "row 11"               "after P110"
      │                      │
      ▼                      ▼
   refers to the          refers to a
   list's state           specific row's
   at query time          own value
   (shifts!)              (doesn't shift!)
```

## 13. The cursor query

Translating "after P110" into SQL is almost embarrassingly simple:

```sql
SELECT * FROM products
WHERE code > 'P110'          -- "everything AFTER the landmark"
ORDER BY code ASC
LIMIT 10;
```

That's it. No `OFFSET`. One `WHERE` clause. Let's verify it against the exact scenario
that broke offset pagination in [§8](#8-failure-mode-1-duplicates).

**Step 1 —** A requests the first page. There's no cursor yet, so the server fetches the
first 10 rows: `P101..P110`. The response includes the cursor for the next page —
`P110`, the last row returned. (Where that cursor *token* comes from is
[§17](#17-why-the-cursor-must-be-opaque-and-versioned); for now, pretend it's the literal
string `P110`.)

**Step 2 —** Another user inserts `P055`. The list becomes:

```
P055 P101 P102 P103 P104 P105 P106 P107 P108 P109 P110 P111 ... P115
```

**Step 3 —** A requests the next page with `cursor=P110`. The server runs:

```sql
SELECT * FROM products WHERE code > 'P110' ORDER BY code LIMIT 10;
```

Which rows match? Every code lexicographically greater than `'P110'`:
`P111, P112, ..., P115`. `P055` does **not** match — it's *before* the landmark. The
insert shifted every position in the list, and the query didn't care, because it asked
for a **value**, not a position.

```
   OFFSET behavior:                    CURSOR behavior:

   insert P055 →                      insert P055 →
   positions shift                    nothing the query
   "row 11" now points                looks at shifts
   at a different row                 code > 'P110' is
   → DUPLICATES                       value-based
                                      → STABLE ✓
```

What about deletions? Suppose `P112` is deleted between page 1 and page 2. The query
`code > 'P110'` returns `P111, P113, P114, P115` — the rows that exist. A deleted row is
simply gone; there is no gap in the list, so there is no gap in the walk. Compare with
offset, where a deletion *before* your position silently skipped rows that still exist.

One more edge case worth calling out: **what if the cursor row itself is deleted?** Say
`P110` is deleted right before A fetches page 2. Does the query break? No — and this is
subtle enough to deserve its own diagram:

```
   Query:  WHERE code > 'P110'

   This does NOT look up a row named P110. It's a value comparison:
   "for each row, is its code greater than the string 'P110'?"

   P111 > 'P110'?  → yes → include
   P109 > 'P110'?  → no  → exclude

   The row P110 does not need to exist. The string 'P110' is all
   the query needs. Cursor survives deletion of its own anchor row. ✓
```

**The requirements on the sort key.** For this to work, the column you sort by must be:

1. **Unique** — or paired with a unique tiebreaker ([§18](#18-when-the-sort-key-is-not-unique-tuple-comparison)).
2. **Comparable** — strings, numbers, timestamps all work.
3. **Immutable** — if a product's `code` could be *changed* after a cursor was issued,
   then `code > 'OLD-VALUE'` might select a different set of rows than the client
   expected. This is one reason `code` should be treated as immutable (it's also in the
   URL path, so it should be anyway).

## 14. What the database actually does: index seek vs scan

From [§4](#4-how-offset-really-workks-inside-the-database): `OFFSET 100000` walks and
discards 100,000 rows. What does `WHERE code > 'P110'` do?

Because `code` is a **unique-indexed** column, PostgreSQL can use the index as a sorted
structure — a B-tree. The index *is* the list, already sorted, with one entry per row:

```
                 B-tree index on code (sorted, always)
                 ┌─────┬─────┬─────┬─────┬─────┬─────┬─────┐
                 │P101 │P102 │P103 │ ... │P109 │P110 │P111 │ ...
                 └─────┴─────┴─────┴─────┴─────┴─────┴─────┘
                                       ▲
                     WHERE code > 'P110'  →  seek directly here
                                             read forward 10 entries
                                             done.

   OFFSET 100000:  start at the beginning of the index,
                   walk 100,000 entries, discard them, then read 10.

   code > 'P110':  binary-search the index for 'P110' (≈ log₂(n) steps),
                   read forward 10 entries. Done.
```

Page 1 and page 10,000 cost the **same**: a logarithmic seek plus a linear read of 10
entries. There is no "deep page" penalty at all, because there is no "deep" — the query
never expresses a depth.

## 15. The "+1 trick": knowing when to stop

Cursor pagination has no `count(*)`, so how does the client know there is a next page?

The elegant answer: **fetch one row more than you need.**

```
Client asks for size = 10.
Server runs       LIMIT 11.        ← one extra row, the "sentinel"

  ┌─────────────────────────────────────┐
  │  got 11 rows?                       │
  │                                     │
  │  YES → a next page exists.          │
  │        return rows 1–10.            │
  │        nextCursor = code of row 10. │
  │        (row 11 is the sentinel —    │
  │         discard it)                 │
  │                                     │
  │  NO (got 10 or fewer) →             │
  │        end of data.                 │
  │        nextCursor = null.           │
  │        return all rows.             │
  └─────────────────────────────────────┘
```

Why this works: the only way to get an 11th row is for the table to contain at least one
row *after* your last returned row. That is *exactly* the definition of "there is a next
page." One indexed query answers both "give me this page" and "is there more?" — the two
questions offset pagination needs two queries for.

```json
{
  "data": [ ...10 products... ],
  "nextCursor": "eyJ2IjoxLCJjb2RlIjoiUDEwOSJ9",
  "size": 10
}
```

`nextCursor: null` is the client's end-of-data signal. Notice what's *gone* from the
response: `totalElements`, `totalPages`, `isFirst`, `isLast`. A cursor cannot honestly
compute those — "page 3 of 4,213" is a position-based concept. If a UI genuinely needs
"4,213 results", run a separate, **cached** count query as a bolted-on feature, not as
part of the paging mechanism.

## 16. The off-by-one trap

The +1 trick hides a trap that has shipped to production many times. The cursor must be
the code of the **last row you returned** — *not* the sentinel row you held back.

Here is the wrong version, and the bug it produces:

```
   WRONG: nextCursor = row 11's code (the sentinel)

   Page 1 returns rows 1–10:  P101..P110
   nextCursor points at:      P111        ← the sentinel, NOT returned

   Page 2 query:  WHERE code > 'P111'
   → returns P112..P121

   💥 P111 was NEVER shown to the client. It was skipped.
```

And the correct version:

```
   CORRECT: nextCursor = row 10's code (the last row RETURNED)

   Page 1 returns rows 1–10:  P101..P110
   nextCursor points at:      P110

   Page 2 query:  WHERE code > 'P110'
   → returns P111..P120      ← P111 is included. No gap. ✓
```

The intuition: the cursor means **"everything strictly after this code."** If the cursor
points at a row the client never saw, everything up to and including that row silently
vanishes from the walk. Rule of thumb: *the cursor always describes the last thing the
client has seen.*

## 17. Why the cursor must be opaque (and versioned)

So far we've pretended the cursor is the literal string `P110`. Real APIs send something
like this:

```
eyJ2IjoxLCJjb2RlIjoiUDEwOSJ9
```

That's `{"v":1,"code":"P109"}` Base64-URL-encoded. Two reasons for the encoding.

### Reason 1: opacity is a contract

If clients can decode the token, they *will* hand-craft it. `{"code":"P109"}` is trivially
editable to `{"code":"P200"}`. The moment one client does that, your cursor format is a
**public API** — you can never change it without breaking that client. Keep it opaque and
the format stays an implementation detail you can evolve freely.

### Reason 2: versioning turns format changes into clean errors

The payload carries a version field:

```json
{"v": 1, "code": "P109"}
```

If the format later needs a new field (say, a timestamp, so cursors can expire after a
day), you bump `v` to 2. Old cursors now fail with a clean, specific error:

```
400 Bad Request
{"title": "Invalid cursor", "detail": "Unsupported cursor version"}
```

instead of silently producing wrong results. A client that stored a cursor for a week
gets a clear "your token is stale, start over" — not a mysteriously broken page.

### The failure path must be a 4xx, not a 5xx

What happens when a client sends `cursor=not-real`? Base64 decoding fails, or the JSON
doesn't parse, or the version is wrong. This is a **client bug** — the client sent
something malformed. So the server must respond **400 Bad Request**, never 500:

```
Client sends garbage cursor
        │
        ▼
ProductCursorCodec.decode() throws InvalidCursorException
        │
        ▼
GlobalExceptionHandler catches it
        │
        ▼
400 Bad Request + ProblemDetail
{"title": "Invalid cursor", "detail": "Malformed cursor", "status": 400}
```

The distinction matters operationally: monitoring tools alert on 5xx (they signal *server*
bugs). A malformed cursor is not a server bug — it should not page anyone at 3 AM.

## 18. When the sort key is not unique: tuple comparison

Everything so far assumed the sort key is unique. What if you want to sort by `price`?
Hundreds of products share a price. The naive query breaks:

```sql
SELECT * FROM products
WHERE price > 42.50          -- 🚨 WRONG
ORDER BY price
LIMIT 10;
```

If the last page ended at a product priced `42.50`, and there are *five* products priced
`42.50`, the query skips the other four — they don't satisfy `price > 42.50`, and they
weren't shown yet.

The fix: add a **unique tiebreaker** and compare *pairs*. PostgreSQL (and MySQL 8+,
SQL Server with some care) support row-value comparisons:

```sql
SELECT * FROM products
WHERE (price, id) > (42.50, 17)      -- "after the pair (price=42.50, id=17)"
ORDER BY price, id
LIMIT 10;
```

This means: rows whose price is greater than 42.50, **or** price exactly 42.50 and id
greater than 17. Walk through it: the last page ended at the product with `price=42.50,
id=17`. The next page must include *the remaining products at that same price* (ids > 17)
and then everything more expensive. Both conditions are captured by the single tuple
comparison. No duplicates, no skips.

The general rule: **cursor pagination sorts by (business key, unique tiebreaker) and
compares tuples.** The tiebreaker is almost always the primary key, because it's unique
and immutable by definition.

## 19. Going backwards: previous-page links

Real UIs have a "previous" button. The same mechanism, mirrored:

```sql
-- forward (next page):  rows strictly after the last row seen
SELECT * FROM products
WHERE code > :lastCodeSeen
ORDER BY code ASC
LIMIT 11;

-- backward (previous page): rows strictly before the first row seen, in reverse
SELECT * FROM products
WHERE code < :firstCodeSeen
ORDER BY code DESC
LIMIT 11;
```

The subtlety: the backward query returns rows in **descending** order, so the response
must be **reversed** before display (or you issue a descending cursor and let the client
reverse). And the "previous cursor" is the code of the **first** row of the current page
— mirror-image of the next cursor being the last row.

This is why production APIs like Stripe return *both* directions:

```json
{
  "data": [ ... ],
  "has_more": true,
  "next_page_url":  "/v1/products?starting_after=P110",
  "prev_page_url":  "/v1/products?ending_before=P101"
}
```

`starting_after` = "give me rows after this landmark" (forward). `ending_before` = "give
me rows before this landmark" (backward). Same mechanism, two directions.

---

# Part 5 — The real world

## 20. How production APIs do it

Cursor pagination is not an academic idea — it's the default for modern public APIs.
Here's how the big ones do it, so you can recognize the pattern in the wild.

### Stripe — payments

Stripe's list endpoints are the canonical example. Every list response carries a
`has_more` flag and two navigation URLs:

```
GET https://api.stripe.com/v1/charges?limit=10

{
  "object": "list",
  "data": [ ...10 charges... ],
  "has_more": true,
  "next_page_url":  "/v1/charges?starting_after=ch_3NxYz...",
  "prev_page_url":  "/v1/charges?ending_before=ch_3NxYz..."
}
```

The cursor value is the **ID of the last charge** — exactly our `code > 'P110'` pattern,
using the primary key as the landmark. `has_more` is our +1 trick's `hasNext`.

### Slack — messages

Slack's `conversations.history` returns:

```json
{
  "messages": [ ... ],
  "has_more": true,
  "response_metadata": {
    "next_cursor": "bmV4dF9jdXJzb3I6..."
  }
}
```

The cursor is an opaque token (ours is Base64; theirs is a different opaque encoding —
same idea). Messages are sorted by timestamp, which is unique enough within a channel.

### Facebook / Instagram — Graph API

Facebook's Graph API pagination is the textbook cursor design:

```json
{
  "data": [ ... ],
  "paging": {
    "cursors": {
      "after": "MTAxNTExOTQ1MjAwNzI5NDE=",
      "before": "NDMyNzQyODI3OTQw"
    },
    "next": "https://graph.facebook.com/v19.0/me/feed?after=MTAxNTEx..."
  }
}
```

Facebook's docs explicitly say why they use cursors rather than offsets: *"Offset-based
pagination ... can skip some photos or return the same photo twice"* when items are added
during pagination. That is [§8](#8-failure-mode-1-duplicates) and
[§9](#9-failure-mode-2-skipped-rows), verbatim, from a company that learned it the hard
way at billions of users.

### GitHub — two APIs, two styles

GitHub is instructive because it ships **both**:

- **REST API**: page-number based (`?page=3&per_page=30`) — the older style. It works
  because most GitHub lists are relatively stable and deep paging is rare.
- **GraphQL API**: cursor-based (Relay-style "connections"), because GraphQL serves
  infinite-scroll UIs on fast-changing data:

```graphql
{
  repository(name: "linux", owner: "torvalds") {
    stargazers(first: 10, after: "Y3Vyc29yOnYyOpHOBAAAAA==") {
      edges { cursor node { login } }
      pageInfo { hasNextPage endCursor }
    }
  }
}
```

Note `pageInfo { hasNextPage endCursor }` — `hasNextPage` is our +1 trick;
`endCursor` is our last-returned-row code. The Relay connection spec is essentially this
tutorial formalized.

### Twitter/X — the scale case

Twitter moved away from offset pagination for timelines partly because of a famous
failure: with tens of millions of tweets per hour, offset paging was both slow (deep
offsets) and unstable (new tweets constantly shifting positions). The fix was cursor
pagination on tweet ID — a monotonically increasing, unique, immutable sort key. Tweet
IDs are designed (Snowflake IDs) to be *exactly* the kind of cursor-friendly key
[§13](#13-the-cursor-query) requires: time-ordered, unique, no gaps.

### Elasticsearch — search_after

Even search engines hit this problem. Elasticsearch deprecated `from`/`size` deep paging
(they cap it at 10,000 by default, precisely because of the walk-and-discard cost) and
replaced it with `search_after`, which is cursor pagination over the sort-key values:

```json
{
  "sort": [{ "timestamp": "asc" }, { "_id": "asc" }],
  "search_after": [1699999999999, "doc_123"],
  "size": 10
}
```

Note the two-element sort: `timestamp` (business key) + `_id` (tiebreaker) — the tuple
comparison of [§18](#18-when-the-sort-key-is-not-unique-tuple-comparison), expressed as a
sort specification.

**The pattern to recognize:** every one of these APIs has (1) an opaque token, (2) a
"has next" signal, and (3) a stable sort key — usually the primary key or a timestamp.

## 21. Side-by-side comparison

| | Offset | Cursor |
|---|---|---|
| Client says | "skip 20 rows, give me 10" | "give me 10 rows after code `P110`" |
| Anchors to | a **position** (shifts when data changes) | a **landmark** (a value that doesn't move) |
| Stable under inserts/deletes | ✗ duplicates / skipped rows | ✓ stable |
| Deep-page cost | walks and discards N rows | index seek — same cost at any depth |
| `COUNT(*)` per request | yes (every page) | no |
| Jump to page 7 | ✓ | ✗ (must walk pages) |
| "N results total" | ✓ | ✗ (only "has next") |
| Response shape | `totalElements`, `totalPages`, `page`, `isFirst`, `isLast`, `hasNext`, `hasPrevious`, `data` | `data`, `nextCursor`, `size` |
| DoS via `?page=999999999` | possible | not possible (no position to abuse) |
| Implementation complexity | trivial | moderate (encoding, +1, edge cases) |
| Typical use | admin screens, reports, "page N of M" UIs | public APIs, infinite scroll, large/changing datasets |
| Users of this style | classic Spring Data `Page`, most internal tools | Stripe, Slack, Facebook, GraphQL/Relay, Elasticsearch `search_after` |

## 22. Which one should you use? A decision guide

```
Is the dataset small (< a few thousand rows) and mostly stable?
  │
  ├─ YES, and users need "jump to page N" or "N results total"
  │     → OFFSET. This is admin screens, back-office lists, reports.
  │       Offset is the right tool here; don't over-engineer.
  │
  └─ NO / not sure:
        │
        ├─ Is this a PUBLIC API that other companies integrate with?
        │     → CURSOR. You cannot control when their jobs run;
        │       offset duplicates/skips will burn them, and you
        │       can't fix it later without breaking them.
        │
        ├─ Is this an infinite-scroll UI (feed, catalog, search results)?
        │     → CURSOR. There is no "page 7" to jump to; the user
        │       just keeps walking. Offset's instability and deep-page
        │       cost bite exactly here.
        │
        ├─ Is the data changing rapidly (new rows every few seconds)?
        │     → CURSOR. Offset instability grows with write rate.
        │
        └─ Could the dataset grow unboundedly (logs, events, messages)?
              → CURSOR. Deep-page cost and DoS vectors grow with it.
```

**The honest summary for this project:** a learning catalog service with 15 products and
pages of 10 → **offset pagination is perfectly fine, and it is kept as-is** in
`ProductService.getAllProducts`. The cursor endpoint
(`GET /api/v1/products/cursor`) exists *to learn the pattern* — it's ~30 lines of code
and teaches a mechanism you will meet in every serious API.

A rule of thumb used by many teams: **default to cursor for anything user-facing or
public; use offset for internal admin screens** where "page 3 of 47" is genuinely useful
and the data is small and stable.

---

# Part 6 — This project

## 23. The implementation, explained line by line

The cursor endpoint is `GET /api/v1/products/cursor?cursor=<token>`. Here is every piece,
with its role.

### The response record

```java
public record CursorPageResponse<T>(List<T> data, String nextCursor, int size) {}
```

A *separate* record from `PageResponse` — deliberately. The two shapes are different
contracts; silently swapping one endpoint's shape would break every existing client
([§21](#21-side-by-side-comparison)). `nextCursor: null` is the end-of-data signal.

### The codec: encoding and decoding the token

```java
@Component
class ProductCursorCodec {

    private static final int CURSOR_VERSION = 1;
    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    String encode(String lastCode) {
        String json = jsonMapper.writeValueAsString(new CursorPayload(CURSOR_VERSION, lastCode));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(json.getBytes());
    }

    String decode(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return null;                          // first page
        }
        try {
            String json = new String(Base64.getUrlDecoder().decode(cursor));
            CursorPayload payload = jsonMapper.readValue(json, CursorPayload.class);
            if (payload.version() != CURSOR_VERSION) {
                throw new InvalidCursorException("Unsupported cursor version");
            }
            return payload.code();
        } catch (Exception e) {
            throw new InvalidCursorException("Malformed cursor");
        }
    }

    private record CursorPayload(@JsonProperty("v") int version, @JsonProperty("code") String code) {}
}
```

- `encode`: `{"v":1,"code":"P109"}` → Base64-URL → `eyJ2IjoxLCJjb2RlIjoiUDEwOSJ9`.
  The `v` field is the version marker from [§17](#17-why-the-cursor-must-be-opaque-and-versioned).
- `decode`: returns `null` for an absent cursor (first page), and throws
  `InvalidCursorException` for any garbage — which the `GlobalExceptionHandler` maps to
  **400 Bad Request**, never 500.
- The codec is package-private: it's an internal implementation detail of the
  `domain.product` package, not part of the public API.

### The repository: the keyset query

```java
@Query("select p from ProductEntity p where p.code > :code order by p.code asc")
List<ProductEntity> findByCodeGreaterThan(@Param("code") String code, Pageable pageable);
```

This is [§13](#13-the-cursor-query) verbatim: `WHERE code > :code ORDER BY code`. The
caller passes `PageRequest.of(0, size + 1, Sort.by("code"))` — the `+1` of
[§15](#15-the-1-trick-knowing-when-to-stop) lives in the pageable, not the query.

### The service: the whole mechanism in six lines

```java
@Transactional(readOnly = true)
public CursorPageResponse<Product> getAllProductsByCursor(String cursor) {
    String lastCode = cursorCodec.decode(cursor);

    List<ProductEntity> rows = productRepository.findByCodeGreaterThan(
            lastCode == null ? "" : lastCode,
            PageRequest.of(0, CURSOR_PAGE_SIZE + 1, Sort.by("code")));

    boolean hasNext = rows.size() > CURSOR_PAGE_SIZE;
    List<Product> data = rows.stream()
            .limit(CURSOR_PAGE_SIZE)
            .map(ProductMapper::toProduct)
            .toList();
    String nextCursor =
            hasNext ? cursorCodec.encode(rows.get(CURSOR_PAGE_SIZE - 1).getCode()) : null;

    return new CursorPageResponse<>(data, nextCursor, data.size());
}
```

Line by line:

1. **Decode** the cursor → `null` means first page.
2. **Query** with `code > lastCode` (or `code > ""` for the first page — every non-empty
   string sorts after the empty string in PostgreSQL, so this is correct for `text`
   columns), fetching `size + 1` rows.
3. **`hasNext`** = we got the sentinel row.
4. **Return** only the first `size` rows — the sentinel is discarded.
5. **`nextCursor`** = code of the **last row returned** (`rows.get(size - 1)`), *not* the
   sentinel — the off-by-one trap of [§16](#16-the-off-by-one-trap).

### The controller

```java
@GetMapping("/cursor")
public CursorPageResponse<Product> getAllProductsByCursor(
        @RequestParam(value = "cursor", required = false) String cursor) {
    return productService.getAllProductsByCursor(cursor);
}
```

### The error path

```java
@ExceptionHandler(InvalidCursorException.class)
public ResponseEntity<ProblemDetail> handleInvalidCursorException(InvalidCursorException exception) {
    ProblemDetail problemDetail =
            ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
    problemDetail.setTitle("Invalid cursor");
    problemDetail.setProperty("timestamp", Instant.now());
    return new ResponseEntity<>(problemDetail, HttpStatus.BAD_REQUEST);
}
```

A malformed cursor is the client's bug → **400**, never 500
([§17](#17-why-the-cursor-must-be-opaque-and-versioned)).

## 24. Experiments you can run

With the app running against the seeded 15 products:

**Experiment 1 — offset produces duplicates; cursor doesn't.**

```bash
# 1. Offset: fetch page 1 (rows 1–10)
curl 'http://localhost:8071/api/v1/products?page=1'
#    → data: [P101..P110]

# 2. Insert a product that sorts BEFORE everything (P055 < P101)
curl -X POST 'http://localhost:8071/api/v1/products' \
  -H 'Content-Type: application/json' \
  -d '{"code":"P055","name":"Inserted item","description":"...","price":9.99}'

# 3. Offset: fetch page 2 → P110 appears AGAIN (duplicate!)
#    It was the last row of page 1; after the insert it is the first row of page 2.
curl 'http://localhost:8071/api/v1/products?page=2'
#    → data: [P110, P111..P115]

# 4. Cursor: first page
curl 'http://localhost:8071/api/v1/products/cursor'
#    → {"data":[P101..P110],"nextCursor":"eyJ2IjoxLCJjb2RlIjoiUDEwOSJ9","size":10}

# 5. Insert P055 again (or don't delete it from step 2), then fetch page 2
#    with the token from step 4 → the exact rows after P110, no duplicates, no gaps
curl 'http://localhost:8071/api/v1/products/cursor?cursor=eyJ2IjoxLCJjb2RlIjoiUDEwOSJ9'
#    → data: [P111..P115]   ← P110 is NOT here
```

**Experiment 2 — a garbage cursor is a clean 400.**

```bash
curl 'http://localhost:8071/api/v1/products/cursor?cursor=not-real'
#    → 400 Bad Request, {"title":"Invalid cursor","detail":"Malformed cursor"}
```

**Experiment 3 — deep pages cost the same for cursor, but not for offset.**

```bash
# time a deep offset page
time curl 'http://localhost:8071/api/v1/products?page=900000'
# (Spring Data will happily accept it — the DB does the walk-and-discard)

# the cursor endpoint has no "deep" concept at all — every page is a seek
time curl 'http://localhost:8071/api/v1/products/cursor?cursor=<any-valid-token>'
```

Run experiments 1–3 and 4–5 side by side and you will *feel* the difference the
comparison table describes.

## 25. Exercises

These are the deliberate simplifications in the current implementation — each is a small,
well-scoped refactor that teaches one more piece of the pattern.

1. **Nullable-cursor predicate.** The first-page query currently starts from `""`
   (correct for `text` in PostgreSQL, but a bit of a trick). Make it explicit: change the
   query to `where (:code is null or p.code > :code)` and pass `null` for the first page.
   This also makes the query portable to databases where `""`-ordering assumptions don't
   hold.
2. **Client-controlled page size.** Add a `size` parameter with `@Min(1) @Max(100)` on
   the `@RequestParam`. In Spring Boot 4, constraints on a bare `@RequestParam` are
   validated automatically (no `@Validated` needed) and violations become a 400. Ask
   yourself: why cap it at 100? (Hint: [§10](#10-failure-mode-3-deep-pages-get-slower-and-slower)
   — a huge `size` is the cursor equivalent of a huge offset.)
3. **Previous-page links.** Add a `prevCursor` by mirroring the query:
   `code < :firstCode ORDER BY code DESC`, and return the *first* row's code as the
   previous cursor ([§19](#19-going-backwards-previous-page-links)). Watch out for the
   response-order reversal.
4. **Non-unique sort.** Implement cursor pagination sorted by `price` using the tuple
   comparison from [§18](#18-when-the-sort-key-is-not-unique-tuple-comparison):
   `WHERE (price, id) > (:lastPrice, :lastId) ORDER BY price, id`. This is the hardest
   exercise and the most instructive — it forces you to think about what "after" means
   when the sort key isn't unique.
5. **Cursor expiry.** Add a timestamp to the cursor payload and reject cursors older
   than 24 hours. This teaches the versioning mechanism of [§17](#17-why-the-cursor-must-be-opaque-and-versioned)
   — bump `v` to 2 when you do it.
