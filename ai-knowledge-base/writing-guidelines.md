# Writing guidelines

Every document in this project (README, assumptions, code comments, commit messages) should read as if a person wrote it. These notes are based on the Wikipedia guide [Signs of AI writing](https://en.wikipedia.org/wiki/Wikipedia:Signs_of_AI_writing), read on 2026-11-18. Parts that only apply to Wikipedia (citations, edit summaries, article review) are left out.

One point from that page matters more than the word lists: the signs are symptoms. Swapping "crucial" for "important" does not fix a paragraph that says nothing specific. Fix the content first, then the wording.

## Rules for our docs

1. Say the specific thing. "Retries up to 3 times with 500 ms, 1 s, 2 s backoff" beats "ensures robust handling of network failures".
2. Use "is" and "are". Write "X is the mock", not "X serves as the mock".
3. Don't add an "-ing" clause to the end of a sentence to explain why it matters ("..., ensuring a smooth experience"). If the reason matters, give it its own sentence with evidence.
4. Don't group things in threes by habit. List as many items as there actually are.
5. No "not just X, but Y", "it's not X, it's Y", or "Y rather than X" as a sentence formula.
6. Call a thing by the same name every time. If it's "the inputs stream", don't later call it "the computation feed".
7. Use bold rarely. Don't start list items with a bold label followed by a colon.
8. Headings in sentence case. One top-level `#` heading per file.
9. No emoji, no em dashes. Use commas, colons, parentheses or full stops.
10. No closing "In summary" or "Overall" paragraph, and no "Challenges and future outlook" section. If there are gaps, list them plainly under a plain heading.
11. Don't talk to the reader like a chatbot: no "I hope this helps", "Let me know", "It's important to note", "Certainly".
12. No placeholders left in (`[Your Name]`, `2026-XX-XX`, `TODO add link`).
13. Prefer plain verbs: used, wrote, tried, moved. Not utilized, authored, attempted, relocated.
14. Avoid vague links between things ("in connection with", "associated with"). Say what the relationship is.
15. Read it out loud. If a sentence sounds like a press release, rewrite it.

## Signs to check for

### Puffing up significance

Phrases: stands as, serves as, is a testament to, is a reminder of, plays a crucial / pivotal / vital / significant / key role, underscores its importance, highlights its significance, reflects broader, symbolizing its ongoing / enduring / lasting, contributing to the, setting the stage for, marking / shaping the, represents a shift, key turning point, evolving landscape, focal point, indelible mark, deeply rooted, has shaped conversations about.

### Shallow analysis tacked on

Sentences ending in an "-ing" phrase: highlighting..., underscoring..., emphasizing..., ensuring..., reflecting..., symbolizing..., contributing to..., fostering..., cultivating..., encompassing..., enhancing.... Also "valuable insights", "align with", "resonate with".

### Vague attributions

"Industry reports", "Observers have cited", "Experts argue", "Some critics argue", "several sources" when only one is cited.

### Overused vocabulary

Many of these in one text is one of the strongest tells. One or two can be coincidence.

Older models: additionally (at the start of a sentence), align with, boasts (meaning "has"), bolstered, crucial, deep dive, delve, emphasizing, enduring, enhance, fostering, garner, highlight (as a verb), interplay, intricate, intricacies, key, landscape (in the abstract sense), meticulous, meticulously, pivotal, robust, showcase, tapestry, testament, underscore, valuable, vibrant.

Newer models (mid-2026 on): challenge, clearer, dependable, echoed, foster, leverage, matters, multifaceted, practical, prioritize, quietly, steady, universally. These models also write lots of short sentences (about 8 to 20 words) with many commas and full stops.

Promotional words: rich, profound, exemplifies, commitment to, groundbreaking, renowned, featuring, diverse array, seamless, seamlessly.

### Avoiding "is" and "are"

Using serves as, stands as, functions as, operates as, represents, marks, boasts, features, offers, refers to where "is" or "has" would do.

### Negative parallelisms

"Not only ... but also ...", "It's not just ..., it's ...", "It's not ..., it's ...", "No ..., no ..., just ...", and the reversed form "Y rather than X".

### Rule of three

Repeated triples ("fast, reliable, and scalable") used to make thin points look thorough.

### Synonym cycling

Rotating labels for the same thing to avoid repeating a word.

### Formatting

Title Case Headings, bold scattered through prose, bullet points that start with a bold label, emoji in headings or lists, heavy em dash use (often with spaces around it), and curly quotes where the rest of the file uses straight ones.

### Chatbot leftovers

"I hope this helps", "Of course!", "Certainly!", "You're absolutely right!", "Would you like...", "Is there anything else", "Let me know", "Here is a more detailed breakdown".

Disclaimers: "It's important to note", "It's important to remember", "While specific details are limited", "based on available information", "... does not by itself establish ...", "should be treated as ... rather than ...".

Knowledge cutoff talk: "As of my last knowledge update". Refusal phrasing: "As an AI language model".

### Formulaic endings

"In summary", "In conclusion", "Overall", a "Conclusion" heading, and "Despite these challenges..." or "Future outlook" sections.

### Placeholders

`[Your Name]`, `(Add link here)`, `(If available)`, `2026-XX-XX`, `INSERT_URL_HERE`.

## What is not a sign

The page says these are unreliable, so there's no need to avoid them:

- correct grammar
- formal or plain writing in general
- ordinary transition words (consequently, notably) used now and then
- a mix of formal and casual tone

## What people tend to write

Simple phrasing ("there is a", "it has a"), plain verbs, direct claims ("is the only", "was the first"), small qualifiers ("probably", "tends to"), and the occasional wordy phrase ("in order to", "the fact that"). Explaining why a choice was made, including what went wrong, also reads as human.
