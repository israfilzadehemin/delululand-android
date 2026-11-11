// A fixture API for design-parity screenshots: the same data drives the web app (vite proxy) and the
// Android app (ParityScreensTest, Robolectric). `node tools/parity/server.mjs` listens on :8080.
import http from 'node:http'
import fs from 'node:fs'

const MIN = 60_000
const now = () => Date.now()
const iso = (offMin) => new Date(now() + offMin * MIN).toISOString()
const doc = (...paras) => ({ type: 'doc', content: paras.map((t) => ({ type: 'paragraph', content: [{ type: 'text', text: t }] })) })
const STATE = { auth: process.env.AUTH !== '0', alone: process.env.ALONE === '1', empty: process.env.EMPTY === '1' }
const HERE = new URL('.', import.meta.url).pathname

const me = {
  id: 1, nickname: 'Emin', avatarUrl: '/api/m/emin.jpg', mood: 'happy', moodSetAt: iso(-120), moodEmoji: '😊', moodLabel: 'Happy',
  timezone: 'Europe/Warsaw', firstName: 'Emin', lastName: 'Israfilzade', draftEmpty: false, discussionDraftEmpty: true,
}
const friend = {
  id: 2, nickname: 'Leyla', avatarUrl: null, mood: 'calm', moodSetAt: iso(-25), moodEmoji: '🍵', moodLabel: 'Calm',
  timezone: 'Asia/Tehran', firstName: 'Leyla', lastName: null,
}
const stamp = { slug: 'tulip', name: 'Tulip', imageUrl: '/api/m/stamp.svg' }
const rows = STATE.empty ? { from: [], to: [] } : {
  from: [
    { id: 11, direction: 'from', state: 'DELIVERED', sentAt: iso(-180), windowEndsAt: iso(-175), arrivesAt: iso(-60), firstLine: 'Dear Emin, the week went by slower than I expected', media: { photos: 2, audios: 1 }, moodEmoji: '🍵', moodLabel: 'Calm', stamp },
    { id: 12, direction: 'from', state: 'IN_TRANSIT', sentAt: iso(-32), windowEndsAt: iso(-27), arrivesAt: iso(28), stamp },
    { id: 10, direction: 'from', state: 'READ', sentAt: iso(-3000), windowEndsAt: iso(-2995), arrivesAt: iso(-2940), readAt: iso(-2900), firstLine: 'The tulips are out', stamp },
  ],
  to: [
    { id: 21, direction: 'to', state: 'IN_REACH', sentAt: iso(-2), windowEndsAt: iso(40), arrivesAt: iso(95), firstLine: 'Saturday works for me', stamp },
    { id: 20, direction: 'to', state: 'DELIVERED', sentAt: iso(-200), windowEndsAt: iso(-195), arrivesAt: iso(-140), firstLine: 'Here is the recipe you asked about', stamp },
    { id: 19, direction: 'to', state: 'READ', sentAt: iso(-5000), windowEndsAt: iso(-4995), arrivesAt: iso(-4940), readAt: iso(-4800), firstLine: 'Good morning from Kraków', stamp },
  ],
}
const home = () => ({
  serverNow: new Date().toISOString(), me, friend: STATE.alone ? null : friend, from: rows.from, to: rows.to,
  unreadCount: 1, discussionsUnseen: 1, newDiscussions: [{ id: 31, title: 'Weekend in Zakopane?', arrivesAt: iso(-30) }],
  emailsUnread: 2, emailsReacted: 1, newEmails: [{ id: 71, threadId: 41, subject: 'Tickets for Saturday', sentAt: iso(-20) }],
  emailCredits: { limit: 100, used: 4, left: 96, renewsAt: iso(600) },
  board: STATE.empty ? null : { version: 7, authorId: 2, createdAt: iso(-120), url: '/api/m/board.png', reaction: 'heart', editedFrom: null, comment: { text: 'this sun is literally me today', createdAt: iso(-100) } },
  boardNew: true, notifyPrefs: { paused: false, enabled: ['letter.arrived'] },
  universe: { id: 1, name: 'Delulutopia', isCreator: true, waiting: STATE.alone },
})
const att = (id, kind, pos, extra = {}) => ({ id: 'a' + id, kind, url: kind === 'audio' ? '/api/m/voice.m4a' : '/api/m/photo.jpg', thumbUrl: '/api/m/photo.jpg', width: 1200, height: 900, durationMs: kind === 'audio' ? 42000 : undefined, position: pos, ...extra })
const letter = (id) => {
  const r = [...rows.from, ...rows.to].find((x) => x.id === id) ?? rows.from[0]
  return {
    ...r, body: doc('Dear Emin,', 'the week went by slower than I expected. The tulips by the window finally opened, and I kept thinking you would laugh at how proud I am of them.', 'Write soon.'),
    attachments: r.direction === 'from' ? [att(1, 'photo', 0), att(2, 'photo', 1), att(3, 'audio', 2)] : [],
    canEdit: r.state === 'IN_REACH', canCancel: r.state === 'IN_REACH', replyTo: r.direction === 'from' ? { id: 20, direction: 'to', sentAt: iso(-200), firstLine: 'Here is the recipe you asked about' } : undefined,
  }
}
const drows = [
  { id: 31, authorId: 2, mine: false, color: 'lemon', state: 'ARRIVED', postedAt: iso(-120), windowEndsAt: iso(-115), arrivesAt: iso(-30), title: 'Weekend in Zakopane?', firstLine: 'We could take the morning train and', commentCount: 4, lastCommentAt: iso(-10), isNew: true, unseenComments: 2, activityAt: iso(-10) },
  { id: 32, authorId: 1, mine: true, color: 'sky', state: 'IN_REACH', postedAt: iso(-1), windowEndsAt: iso(40), arrivesAt: iso(155), title: 'Books for the winter', firstLine: 'A list, in no order', isNew: false, unseenComments: 0, activityAt: iso(-1) },
  { id: 33, authorId: 2, mine: false, color: 'pink', state: 'IN_TRANSIT', postedAt: iso(-20), windowEndsAt: iso(-15), arrivesAt: iso(40), isNew: false, unseenComments: 0 },
  { id: 30, authorId: 1, mine: true, color: 'mint', state: 'ARRIVED', postedAt: iso(-3000), windowEndsAt: iso(-2995), arrivesAt: iso(-2940), title: 'Plants', firstLine: 'The basil survived', commentCount: 9, lastCommentAt: iso(-900), isNew: false, unseenComments: 0, media: { photos: 1, audios: 0 }, activityAt: iso(-900) },
]
const comments = [
  { id: 301, authorId: 1, text: 'Yes! Let me check the trains.', createdAt: iso(-60), reactions: [{ emoji: '❤️', userIds: [2] }] },
  { id: 302, authorId: 2, text: 'The 7:10 one is the scenic route', createdAt: iso(-30) },
  { id: 303, authorId: 1, text: 'Booked.', createdAt: iso(-10), editedAt: iso(-8) },
]
const dfull = (id) => {
  const r = drows.find((x) => x.id === id) ?? drows[0]
  return { ...r, title: r.title ?? 'Untitled', body: doc('We could take the morning train and stay two nights.', 'I found a small place near the market.'), attachments: [att(9, 'photo', 0)], canCancel: r.state === 'IN_REACH', comments: { items: comments, olderCursor: null }, reactions: [{ emoji: '😍', userIds: [1] }], canEdit: r.mine, canDelete: r.mine, canArchive: r.state === 'ARRIVED', canUnarchive: false }
}
const threads = [
  { id: 41, subject: 'Tickets for Saturday', startedBy: 2, createdAt: iso(-300), lastEmailAt: iso(-20), lastSenderId: 2, lastText: 'Got them! Row 7, seats 12 and 13. Bring the scarf', emailCount: 4, unread: 2 },
  { id: 42, subject: 'Recipe, finally', startedBy: 1, createdAt: iso(-400), lastEmailAt: iso(-112), lastSenderId: 1, lastMedia: { photos: 0, audios: 0, videos: 1 }, emailCount: 2, unread: 0, unseenReactions: 1, lastReaction: { userId: 2, emoji: '🤩', target: 'email', onMine: true, media: 'video', at: iso(-60) } },
  { id: 43, subject: 'Our playlist', startedBy: 2, createdAt: iso(-5000), lastEmailAt: iso(-4000), lastSenderId: 2, lastText: 'Added three songs, one of them is a joke, guess which', emailCount: 9, unread: 0 },
  { id: 44, subject: 'Passport photos', startedBy: 1, createdAt: iso(-8000), lastEmailAt: iso(-7000), lastSenderId: 1, lastMedia: { photos: 2, audios: 0, videos: 0 }, emailCount: 3, unread: 0 },
]
const emails = [
  { id: 74, threadId: 41, senderId: 2, text: 'Got them! Row 7, seats 12 and 13. Bring the scarf', sentAt: iso(-20), attachments: [att(5, 'photo', 0)], reactions: [] },
  { id: 73, threadId: 41, senderId: 1, text: 'Can you check if there are two seats together?', sentAt: iso(-90), readAt: iso(-60), attachments: [], reactions: [{ emoji: '👍', userIds: [2] }] },
  { id: 72, threadId: 41, senderId: 2, text: 'There is a concert on Saturday, want to go?', sentAt: iso(-200), attachments: [att(6, 'audio', 0)] },
]
const moods = { limit: 48, moods: ['😊 Happy', '🍵 Calm', '😴 Sleepy', '🥰 Loving', '😢 Sad', '😤 Grumpy', '🤒 Sick', '🎉 Excited', '😌 Relaxed', '🤔 Thoughtful', '😰 Anxious', '😎 Cool'].map((s, i) => ({ key: s.split(' ')[1].toLowerCase(), emoji: s.split(' ')[0], label: s.split(' ')[1], custom: i > 10 })) }
const notif = { paused: false, sections: [
  { key: 'letters', enabled: true, events: [{ key: 'letter.arrived', enabled: true, defaultOn: true }, { key: 'letter.incoming', enabled: false, defaultOn: false }, { key: 'letter.read', enabled: true, defaultOn: true }] },
  { key: 'discussions', enabled: true, events: [{ key: 'discussion.arrived', enabled: true, defaultOn: true }, { key: 'discussion.comment', enabled: true, defaultOn: true }, { key: 'discussion.reaction', enabled: false, defaultOn: true }] },
  { key: 'emails', enabled: false, events: [{ key: 'email.new', enabled: true, defaultOn: true }, { key: 'email.reaction', enabled: true, defaultOn: true }] },
] }
const stampSvg = '<svg xmlns="http://www.w3.org/2000/svg" width="60" height="72" viewBox="0 0 60 72"><rect width="60" height="72" fill="#fbf7ee"/><rect x="5" y="5" width="50" height="62" fill="#e3b8a6"/><circle cx="30" cy="34" r="12" fill="#b23a3a"/></svg>'
const stamps = { themes: [{ slug: 'flowers', name: 'Flowers', sort: 1 }, { slug: 'cities', name: 'Cities', sort: 2 }], stamps: Array.from({ length: 12 }, (_, i) => ({ slug: 's' + i, name: 'Stamp ' + i, imageUrl: '/api/m/stamp.svg', keywords: [], theme: i % 2 ? 'flowers' : 'cities', sort: i })), recent: ['s1', 's2'] }

const routes = [
  ['GET', /^\/api\/config$/, () => ({ delay: 'PT60M', window: 'PT5M', discussionDelay: 'PT120M', testTiming: false, vapidPublicKey: null })],
  ['GET', /^\/api\/users$/, () => [{ id: 1, nickname: 'Emin', avatarUrl: '/api/m/emin.jpg', hasPin: true }, { id: 2, nickname: 'Leyla', avatarUrl: null, hasPin: true }]],
  ['GET', /^\/api\/universes?$/, () => ({ name: 'Delulutopia', welcome: 'Welcome to our little universe.\n\nLetters take an hour.', id: 1 })],
  ['GET', /^\/api\/universe$/, () => ({ id: 1, name: 'Delulutopia', welcome: 'Welcome to our little universe.\n\nLetters take an hour to arrive, so write slowly.', creator: { id: 1, username: 'Emin' }, isCreator: process.env.READER !== '1', codeRotatedAt: iso(-60 * 24 * 5) })],
  ['GET', /^\/api\/universe\/entry$/, () => ({ name: 'Delulutopia', welcome: 'Welcome to our little universe.\n\nLetters take an hour to arrive, so write slowly.', canJoin: false, users: [{ id: 1, nickname: 'Emin', avatarUrl: '/api/m/emin.jpg', hasPin: true }, { id: 2, nickname: 'Leyla', avatarUrl: null, hasPin: true }] })],
  ['GET', /^\/api\/home\/counts$/, () => ({ unreadCount: 1, discussionsUnseen: 1, emailsUnread: 2, emailsReacted: 1, boardNew: true })],
  ['GET', /^\/api\/home$/, () => (STATE.auth ? home() : [401, { code: 'NOT_LOGGED_IN', message: 'Log in' }])],
  ['GET', /^\/api\/letters\/(\d+)$/, (m) => letter(+m[1])],
  ['GET', /^\/api\/letters/, () => ({ items: STATE.empty ? [] : [...rows.from, ...rows.to].sort((a, b) => b.sentAt.localeCompare(a.sentAt)), nextCursor: null })],
  ['GET', /^\/api\/draft$/, () => ({ body: doc('Dear Leyla,', 'the trains are booked. I will bring the scarf, promise.'), attachments: [att(7, 'photo', 0), att(8, 'audio', 1)], pinnedLetterId: 11, stamp, version: 3, savedAt: iso(-1) })],
  ['GET', /^\/api\/stamps/, () => stamps],
  ['GET', /^\/api\/moods/, () => moods],
  ['GET', /^\/api\/discussions\/draft$/, () => ({ title: '', color: 'lemon', body: doc(), attachments: [], version: 1, savedAt: null })],
  ['GET', /^\/api\/discussions\/(\d+)\/comments/, () => ({ items: comments, olderCursor: null })],
  ['GET', /^\/api\/discussions\/(\d+)$/, (m) => dfull(+m[1])],
  ['GET', /^\/api\/discussions/, (m, u) => ({ items: STATE.empty || u.includes('archived=true') ? [] : drows, nextCursor: null, archivedCount: 2 })],
  ['GET', /^\/api\/emails\/credits$/, () => ({ limit: 100, used: 4, left: process.env.NOCREDITS ? 0 : 96, renewsAt: iso(600) })],
  ['GET', /^\/api\/emails\/drafts\//, () => ({ subject: '', text: '', attachments: [], version: 1, savedAt: null })],
  ['GET', /^\/api\/emails\/threads\/(\d+)\/emails/, () => ({ items: [], olderCursor: null })],
  ['GET', /^\/api\/emails\/threads\/(\d+)$/, (m) => ({ thread: threads.find((t) => t.id === +m[1]) ?? threads[0], emails: { items: emails, olderCursor: 'x' } })],
  ['GET', /^\/api\/emails\/threads/, () => ({ items: STATE.empty ? [] : threads, nextCursor: null })],
  ['GET', /^\/api\/me\/profile$/, () => ({ id: 1, username: 'Emin', firstName: 'Emin', lastName: 'Israfilzade', timezone: 'Europe/Warsaw', avatarUrl: '/api/m/emin.jpg' })],
  ['GET', /^\/api\/me\/notifications$/, () => notif],
  ['GET', /^\/api\/live/, () => [204, null]],
]
const MEDIA = { 'board.png': 'image/png', 'emin.jpg': 'image/jpeg', 'leyla.jpg': 'image/jpeg', 'photo.jpg': 'image/jpeg' }

http.createServer((req, res) => {
  const url = req.url
  const path = url.split('?')[0]
  // Test hooks: flip the fixture state (signed out, alone, empty) between screenshots.
  if (path === '/__mock/state') {
    const q = new URLSearchParams(url.split('?')[1] ?? '')
    for (const k of ['auth', 'alone', 'empty']) if (q.has(k)) STATE[k] = q.get(k) === '1'
    res.writeHead(204)
    return res.end()
  }
  if (path.startsWith('/api/m/')) {
    const f = path.slice(7)
    if (f === 'stamp.svg') { res.writeHead(200, { 'content-type': 'image/svg+xml' }); return res.end(stampSvg) }
    if (MEDIA[f]) { res.writeHead(200, { 'content-type': MEDIA[f] }); return res.end(fs.readFileSync(HERE + f)) }
    res.writeHead(404); return res.end()
  }
  for (const [method, re, fn] of routes) {
    const m = path.match(re)
    if (m && (req.method === method)) {
      let out = fn(m, url)
      let status = 200
      if (Array.isArray(out) && typeof out[0] === 'number' && out.length === 2 && !Array.isArray(out[1])) [status, out] = out
      res.writeHead(status, { 'content-type': 'application/json', 'x-server-time': new Date().toISOString() })
      return res.end(out == null ? '' : JSON.stringify(out))
    }
  }
  if (req.method !== 'GET') { res.writeHead(204); return res.end() }
  res.writeHead(404, { 'content-type': 'application/json' }); res.end(JSON.stringify({ code: 'NOT_FOUND', message: 'Not found ' + path }))
}).listen(8080, () => console.log('mock on 8080'))
