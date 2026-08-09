<!-- Fonte: https://code.claude.com/docs/pt/prompt-library | Baixado em: 2026-08-06 | slug: prompt-library -->

# Biblioteca de prompts

> Copie e cole prompts para Claude Code, marcados por tarefa e função.

export const PromptLibrary = ({text = {}, labels = {}, tagLabels = {}, phaseLabels = {}, sourceLabels = {}, catLabels = {}}) => {
  const RAW = useMemo(() => [{
    id: 'get-oriented-in-a',
    sdlc: 'discover',
    cat: 'Onboard',
    startN: 1,
    roles: [],
    prompt: 'give me an overview of this codebase: architecture, key directories, and how the pieces connect',
    nextHref: '/en/memory',
    src: 'workflows'
  }, {
    id: 'explain-unfamiliar-code',
    sdlc: 'discover',
    cat: 'Understand',
    roles: [],
    prompt: 'explain what {path} does and how data flows through it. write it up as {format}',
    slots: {
      path: 'src/scheduler/queue.ts',
      format: 'an HTML page with a diagram, then open it in my browser'
    },
    nextHref: '/en/output-styles',
    src: 'workflows'
  }, {
    id: 'find-where-something-happens',
    sdlc: 'discover',
    cat: 'Understand',
    startN: 2,
    roles: [],
    prompt: 'where do we {behavior}?',
    slots: {
      behavior: 'validate uploaded file types'
    },
    src: 'workflows'
  }, {
    id: 'see-what-depends-on',
    sdlc: 'discover',
    cat: 'Understand',
    roles: [],
    prompt: 'what would break if I deleted {target}?',
    slots: {
      target: 'the retryWithBackoff helper'
    },
    src: 'workflows'
  }, {
    id: 'trace-how-code-evolved',
    sdlc: 'discover',
    cat: 'Understand',
    roles: [],
    prompt: 'look through the commit history of {path} and summarize how it evolved and why',
    slots: {
      path: 'internal/auth/session.go'
    },
    src: 'best-practices'
  }, {
    id: 'scope-a-change-before',
    sdlc: 'discover',
    cat: 'Understand',
    roles: ['pm', 'design'],
    prompt: 'which files would I need to touch to {change}?',
    slots: {
      change: 'add a dark mode toggle to settings'
    },
    src: 'teams'
  }, {
    id: 'ask-the-codebase-a',
    sdlc: 'discover',
    cat: 'Understand',
    roles: ['pm'],
    prompt: 'I am a {role}. walk me through what happens when a user {action}, from the UI down to the result',
    slots: {
      role: 'PM',
      action: 'clicks Export to PDF'
    },
    nextHref: '/en/output-styles',
    src: 'teams'
  }, {
    id: 'plan-a-multi-file',
    sdlc: 'design',
    cat: 'Plan',
    roles: ['pm', 'design'],
    prompt: 'plan how to refactor the {target} to {goal}. list the files you would change, but don\'t edit anything yet',
    slots: {
      target: 'payment module',
      goal: 'support multiple currencies'
    },
    src: 'workflows'
  }, {
    id: 'draft-a-spec-by',
    sdlc: 'design',
    cat: 'Plan',
    roles: ['pm'],
    prompt: 'I want to build {feature}. interview me about implementation, UX, edge cases, and tradeoffs until we have covered everything, then write the spec to SPEC.md',
    slots: {
      feature: 'per-workspace rate limits'
    },
    nextHref: '/en/skills',
    src: 'best-practices'
  }, {
    id: 'turn-a-meeting-into',
    sdlc: 'design',
    cat: 'Plan',
    roles: ['pm'],
    prompt: 'read {input} and write up the action items, then create a {tracker} ticket for each with acceptance criteria',
    slots: {
      input: '@meeting-notes.md',
      tracker: 'Linear'
    },
    needs: 'tracker',
    nextHref: '/en/skills',
    src: 'teams'
  }, {
    id: 'map-edge-cases-before',
    sdlc: 'design',
    cat: 'Plan',
    roles: ['design', 'pm'],
    prompt: 'list the error states, empty states, and edge cases for {feature} that the design needs to cover',
    slots: {
      feature: 'the file upload flow'
    },
    src: 'teams'
  }, {
    id: 'turn-a-mockup-into',
    sdlc: 'design',
    cat: 'Prototype',
    roles: ['design', 'pm', 'marketing'],
    paste: 'mockup',
    prompt: 'here is a mockup. build a working prototype I can click through, matching the layout and states shown',
    src: 'teams'
  }, {
    id: 'implement-from-a-screenshot',
    sdlc: 'design',
    cat: 'Prototype',
    roles: ['design'],
    paste: 'design',
    needs: 'browser',
    prompt: 'implement this design, then take a screenshot of the result, compare it to the original, and fix any differences',
    nextHref: '/en/goal',
    src: 'best-practices'
  }, {
    id: 'follow-an-existing-pattern',
    sdlc: 'build',
    cat: 'Implement',
    roles: [],
    prompt: 'look at how {example} is implemented to understand the pattern, then build {new} the same way',
    slots: {
      example: 'the GitHub webhook handler',
      new: 'a Stripe webhook handler'
    },
    nextHref: '/en/memory',
    src: 'best-practices'
  }, {
    id: 'generate-docs-for-code',
    sdlc: 'build',
    cat: 'Implement',
    roles: ['docs'],
    prompt: 'find {scope} without {format} comments and add them, matching the style already used in the file',
    slots: {
      scope: 'the public functions in src/auth/',
      format: 'JSDoc'
    },
    src: 'workflows'
  }, {
    id: 'add-a-small-well',
    sdlc: 'build',
    cat: 'Implement',
    roles: [],
    prompt: 'add a {endpoint} endpoint that returns {payload}',
    slots: {
      endpoint: '/health',
      payload: 'the app version and uptime'
    },
    src: 'workflows'
  }, {
    id: 'build-a-small-internal',
    sdlc: 'build',
    cat: 'Implement',
    roles: ['pm', 'design', 'marketing', 'docs'],
    prompt: 'create a {tool} using HTML, CSS, and vanilla JavaScript, then open it in my browser',
    slots: {
      tool: 'drag-and-drop Kanban board with three columns'
    },
    src: 'teams'
  }, {
    id: 'work-an-issue-end',
    sdlc: 'build',
    cat: 'Implement',
    roles: [],
    prompt: 'read issue #{issue}, implement the fix, and run the tests',
    slots: {
      issue: '312'
    },
    needs: 'gh',
    src: 'workflows'
  }, {
    id: 'find-and-update-copy',
    sdlc: 'build',
    cat: 'Implement',
    roles: ['design', 'docs', 'marketing'],
    prompt: 'find every place we say "{copy}" or a close variant, show me each one in context, then update them all to "{new}". leave tests and the changelog alone',
    slots: {
      copy: 'Sign up free',
      new: 'Start free trial'
    },
    src: 'teams'
  }, {
    id: 'draft-from-past-examples',
    sdlc: 'build',
    cat: 'Implement',
    roles: ['docs', 'marketing', 'pm'],
    prompt: 'read the {examples} in {folder} to learn the structure and voice, then draft a new one for {topic}',
    slots: {
      examples: 'privacy impact assessments',
      folder: 'legal/pia/',
      topic: 'the new analytics integration'
    },
    nextHref: '/en/skills',
    src: 'legal'
  }, {
    id: 'write-tests-run-them',
    sdlc: 'build',
    cat: 'Test',
    startN: 4,
    roles: [],
    prompt: 'write tests for {path}, run them, and fix any failures',
    slots: {
      path: 'app/parsers/feed.py'
    },
    nextHref: '/en/memory',
    src: 'workflows'
  }, {
    id: 'drive-implementation-from-tests',
    sdlc: 'build',
    cat: 'Test',
    roles: [],
    prompt: 'write tests for {feature} first, then implement it until they pass',
    slots: {
      feature: 'the password reset flow'
    },
    src: 'ebook'
  }, {
    id: 'fill-gaps-from-a',
    sdlc: 'build',
    cat: 'Test',
    roles: [],
    prompt: 'read {report} and add tests for the lowest-covered files until each is above {target}%',
    slots: {
      report: 'coverage/coverage-summary.json',
      target: '80'
    },
    nextHref: '/en/goal',
    src: 'workflows'
  }, {
    id: 'migrate-a-pattern-across',
    sdlc: 'build',
    cat: 'Refactor',
    roles: [],
    prompt: 'migrate everything from {from} to {to}: identify every place that needs to change, then make the changes',
    slots: {
      from: 'the old logging API',
      to: 'the structured logger'
    },
    src: 'workflows'
  }, {
    id: 'port-code-between-languages',
    sdlc: 'build',
    cat: 'Refactor',
    roles: [],
    prompt: 'port {source} to {target}, keeping the same {keep}',
    slots: {
      source: 'this Python module',
      target: 'Rust',
      keep: 'public API and test behavior'
    },
    src: 'teams'
  }, {
    id: 'optimize-against-a-measurable',
    sdlc: 'build',
    cat: 'Refactor',
    roles: ['data'],
    prompt: 'optimize {target} to bring {metric} from {current} down to under {goal}',
    slots: {
      target: 'the search query',
      metric: 'p95 latency',
      current: '2s',
      goal: '500ms'
    },
    nextHref: '/en/goal',
    src: 'ebook'
  }, {
    id: 'fix-a-precise-visual',
    sdlc: 'build',
    cat: 'Refactor',
    roles: ['design'],
    prompt: 'the {element} extends {amount} beyond the {container} on {viewport}. fix it.',
    slots: {
      element: 'login button',
      amount: '20px',
      container: 'card border',
      viewport: 'mobile'
    },
    nextHref: '/en/desktop#preview-your-app',
    src: 'ebook'
  }, {
    id: 'review-your-changes-before',
    sdlc: 'build',
    cat: 'Review',
    startN: 5,
    roles: [],
    prompt: 'review my uncommitted changes and flag anything that looks risky before I commit',
    nextHref: '/en/commands',
    src: 'workflows'
  }, {
    id: 'review-a-pull-request',
    sdlc: 'build',
    cat: 'Review',
    roles: [],
    prompt: 'review PR #{pr} and summarize what changed, then list any concerns',
    slots: {
      pr: '247'
    },
    needs: 'gh',
    nextHref: '/en/code-review',
    src: 'workflows'
  }, {
    id: 'review-infrastructure-changes-before',
    sdlc: 'build',
    cat: 'Review',
    roles: ['security', 'ops'],
    paste: 'plan',
    prompt: 'here is my Terraform plan output. what is this going to do, and is anything here going to cause problems?',
    src: 'teams'
  }, {
    id: 'run-a-security-review',
    sdlc: 'build',
    cat: 'Review',
    roles: ['security'],
    prompt: 'use a subagent to review {path} for security issues and report what it finds',
    slots: {
      path: 'src/api/'
    },
    nextHref: '/en/sub-agents',
    src: 'best-practices'
  }, {
    id: 'review-content-before-sending',
    sdlc: 'build',
    cat: 'Review',
    roles: ['marketing', 'docs'],
    prompt: 'review {file} for {concerns} and list anything I should fix before it goes to {reviewer}',
    slots: {
      file: 'launch-post.md',
      concerns: 'unsupported claims, missing attributions, and brand-guideline issues',
      reviewer: 'legal'
    },
    nextHref: '/en/skills',
    src: 'legal'
  }, {
    id: 'course-correct-a-wrong',
    sdlc: 'build',
    cat: 'Steer',
    roles: [],
    prompt: 'that is not right: {feedback}. try a different approach',
    slots: {
      feedback: 'the function signature needs to stay backward-compatible'
    },
    nextHref: '/en/checkpointing',
    src: 'best-practices'
  }, {
    id: 'narrow-the-scope-of',
    sdlc: 'build',
    cat: 'Steer',
    roles: [],
    prompt: 'that is too much. keep only the changes to {scope} and undo your other edits',
    slots: {
      scope: 'the validation logic in src/forms/'
    },
    src: 'best-practices'
  }, {
    id: 'turn-a-correction-into',
    sdlc: 'build',
    cat: 'Steer',
    roles: [],
    prompt: 'you keep {mistake}. add a rule to CLAUDE.md so this stops happening',
    slots: {
      mistake: 'using default exports when this project uses named exports'
    },
    nextHref: '/en/memory',
    src: 'best-practices'
  }, {
    id: 'resolve-merge-conflicts',
    sdlc: 'ship',
    cat: 'Git',
    roles: [],
    prompt: 'resolve the merge conflicts in this branch and explain what you kept from each side',
    src: 'workflows'
  }, {
    id: 'commit-with-a-generated',
    sdlc: 'ship',
    cat: 'Git',
    roles: [],
    prompt: 'commit these changes with a message that summarizes what I did',
    src: 'workflows'
  }, {
    id: 'open-a-pull-request',
    sdlc: 'ship',
    cat: 'Git',
    roles: [],
    prompt: 'find the {tracker} ticket about {topic} and open a PR that implements it',
    slots: {
      tracker: 'Linear',
      topic: 'the login timeout'
    },
    needs: 'tracker',
    src: 'workflows'
  }, {
    id: 'draft-release-notes-from',
    sdlc: 'ship',
    cat: 'Release',
    roles: ['pm', 'docs', 'marketing'],
    prompt: 'compare {from} to {to} and draft release notes grouped by feature, fix, and breaking change',
    slots: {
      from: 'v2.3.0',
      to: 'v2.4.0'
    },
    nextHref: '/en/skills',
    src: 'workflows'
  }, {
    id: 'write-a-ci-workflow',
    sdlc: 'ship',
    cat: 'Release',
    roles: ['ops'],
    prompt: 'write a GitHub Actions workflow that {steps} on every push to {branch}',
    slots: {
      steps: 'runs the tests and deploys to staging',
      branch: 'main'
    },
    src: 'workflows'
  }, {
    id: 'find-and-fix-a',
    sdlc: 'operate',
    cat: 'Debug',
    startN: 3,
    roles: [],
    prompt: 'the {test} test is failing, find out why and fix it',
    slots: {
      test: 'UserAuth'
    },
    src: 'workflows'
  }, {
    id: 'investigate-a-reported-error',
    sdlc: 'operate',
    cat: 'Debug',
    roles: ['ops'],
    prompt: 'users are seeing {symptom} on {where}. investigate and tell me what is going on',
    slots: {
      symptom: '500 errors',
      where: '/api/settings'
    },
    nextHref: '/en/web-quickstart#pre-fill-sessions',
    src: 'workflows'
  }, {
    id: 'fix-a-build-error',
    sdlc: 'operate',
    cat: 'Debug',
    roles: ['ops'],
    paste: 'error',
    prompt: 'here is a build error. fix the root cause and verify the build succeeds',
    src: 'best-practices'
  }, {
    id: 'investigate-a-production-incident',
    sdlc: 'operate',
    cat: 'Incident',
    roles: ['ops', 'security'],
    prompt: '{symptom}. check the logs, recent deploys, and config changes, then tell me the most likely cause',
    slots: {
      symptom: 'the checkout endpoint started returning 500s an hour ago'
    },
    nextHref: '/en/mcp',
    src: 'workflows'
  }, {
    id: 'diagnose-from-a-console',
    sdlc: 'operate',
    cat: 'Incident',
    roles: ['ops', 'data'],
    paste: 'screenshot',
    prompt: 'here is a screenshot of {console}. walk me through why {resource} is failing and give me the exact commands to fix it',
    slots: {
      console: 'the GCP Kubernetes dashboard',
      resource: 'this pod'
    },
    src: 'teams'
  }, {
    id: 'query-logs-in-plain',
    sdlc: 'operate',
    cat: 'Incident',
    roles: ['security', 'ops', 'data'],
    prompt: 'show me all {events} for {scope} over {timeframe}. write the query, run it, and tell me what stands out',
    slots: {
      events: 'failed logins',
      scope: 'the auth service',
      timeframe: 'the past 24 hours'
    },
    needs: 'db',
    src: 'cybersecurity'
  }, {
    id: 'analyze-a-data-file',
    sdlc: 'operate',
    cat: 'Data',
    roles: ['data', 'pm', 'marketing'],
    paste: 'csv',
    prompt: 'read {file}, summarize the key patterns, and write the results to {output}',
    slots: {
      file: '@reports/q1-signups.csv',
      output: 'an HTML page with charts, then open it in my browser'
    },
    nextHref: '/en/mcp',
    src: 'teams'
  }, {
    id: 'generate-variations-from-performance',
    sdlc: 'operate',
    cat: 'Data',
    roles: ['marketing', 'data'],
    paste: 'csv',
    prompt: 'read {file}, find the underperforming {items}, and generate {n} new variations that stay under {limit} characters',
    slots: {
      file: '@ads-performance.csv',
      items: 'headlines',
      n: '20',
      limit: '90'
    },
    nextHref: '/en/mcp',
    src: 'teams'
  }, {
    id: 'turn-a-recurring-task',
    sdlc: 'operate',
    cat: 'Automate',
    roles: [],
    prompt: 'create a /{name} skill for this project that {steps}',
    slots: {
      name: 'ship',
      steps: 'runs the linter and tests, then drafts a commit message'
    },
    src: 'workflows'
  }, {
    id: 'add-a-hook-for',
    sdlc: 'operate',
    cat: 'Automate',
    roles: [],
    prompt: 'write a hook that {action} after every {event}',
    slots: {
      action: 'runs prettier',
      event: 'edit to a .ts or .tsx file'
    },
    src: 'best-practices'
  }, {
    id: 'connect-a-tool-with',
    sdlc: 'operate',
    cat: 'Automate',
    roles: [],
    prompt: 'set up the {server} MCP server so you can read my {data} directly',
    slots: {
      server: 'Sentry',
      data: 'error reports'
    },
    src: 'workflows'
  }, {
    id: 'capture-what-to-remember',
    sdlc: 'operate',
    cat: 'Automate',
    roles: ['pm', 'docs'],
    prompt: 'summarize what we did this session and suggest what to add to CLAUDE.md',
    src: 'teams'
  }], []);
  const PROMPTS = useMemo(() => {
    if (typeof window !== 'undefined') {
      const rawIds = new Set(RAW.map(p => p.id));
      RAW.forEach(p => {
        if (!text[p.id]) console.warn('[prompt-library] no text[] entry for id:', p.id);
      });
      Object.keys(text).forEach(k => {
        if (!rawIds.has(k)) console.warn('[prompt-library] orphaned text[] key:', k);
      });
    }
    return RAW.map(p => ({
      ...p,
      title: p.id,
      teaches: '',
      ...text[p.id] || ({})
    }));
  }, [RAW, text]);
  const L = labels;
  const TL = k => tagLabels[k] || k;
  const CAT_TAG = useMemo(() => ({
    Onboard: 'understand',
    Understand: 'understand',
    Plan: 'plan',
    Prototype: 'prototype',
    Implement: 'build',
    Test: 'test',
    Refactor: 'refactor',
    Review: 'review',
    Steer: 'steer',
    Git: 'git',
    Release: 'release',
    Debug: 'debug',
    Incident: 'debug',
    Data: 'data',
    Automate: 'automate'
  }), []);
  const TAGS = useMemo(() => ['understand', 'plan', 'prototype', 'build', 'test', 'refactor', 'review', 'steer', 'debug', 'git', 'release', 'data', 'automate', 'pm', 'design', 'docs', 'marketing', 'security', 'ops'], []);
  const tagsOf = p => [CAT_TAG[p.cat], ...p.roles || []];
  const doc = useMemo(() => {
    const p = typeof window !== 'undefined' ? window.location.pathname : '';
    const base = p.startsWith('/docs/') ? '/docs' : '';
    const m = p.slice(base.length).match(/^\/([a-z]{2}(?:-[A-Z]{2})?)\//);
    const locale = m ? m[1] : 'en';
    return href => {
      if (!href || href[0] !== '/' || href[1] === '/') return href;
      return base + (href.startsWith('/en/') ? '/' + locale + href.slice(3) : href);
    };
  }, []);
  const linkify = s => {
    const out = [];
    let last = 0;
    const re = /\[([^\]]+)\]\(([^)]+)\)/g;
    for (let m; m = re.exec(s); ) {
      if (m.index > last) out.push(s.slice(last, m.index));
      out.push(<a key={m.index} href={doc(m[2])}>{m[1]}</a>);
      last = re.lastIndex;
    }
    if (last < s.length) out.push(s.slice(last));
    return out;
  };
  const codeify = s => s.split(/(`[^`]+`)/g).map((part, i) => part[0] === '`' ? <code key={i}>{part.slice(1, -1)}</code> : part);
  const SOURCES = useMemo(() => ({
    'workflows': '/en/common-workflows',
    'teams': 'https://claude.com/blog/how-anthropic-teams-use-claude-code',
    'legal': 'https://claude.com/blog/how-anthropic-uses-claude-legal',
    'cybersecurity': 'https://claude.com/blog/how-anthropic-uses-claude-cybersecurity',
    'best-practices': '/en/best-practices',
    'ebook': 'https://resources.anthropic.com/hubfs/Scaling%20agentic%20coding%20across%20your%20organization.pdf'
  }), []);
  const [mounted, setMounted] = useState(false);
  const [q, setQ] = useState('');
  const [start, setStart] = useState(true);
  const [sel, setSel] = useState(null);
  const [openId, setOpenId] = useState(null);
  const [copied, setCopied] = useState(null);
  const [fills, setFills] = useState({});
  const copyTimer = useRef(null);
  useEffect(() => {
    setMounted(true);
    return () => clearTimeout(copyTimer.current);
  }, []);
  const setFill = (id, key, val) => setFills(f => ({
    ...f,
    [id + '.' + key]: val
  }));
  const fillOf = (p, key) => {
    const v = fills[p.id + '.' + key];
    return v !== undefined ? v : p.slots && p.slots[key] !== undefined ? p.slots[key] : '';
  };
  const assemble = p => p.prompt.replace(/\{(\w+)\}/g, (_, k) => fillOf(p, k) || p.slots && p.slots[k] || k);
  const preview = p => p.prompt.replace(/\{(\w+)\}/g, (_, k) => p.slots && p.slots[k] || k);
  const bodyText = p => preview(p) + ' ' + p.teaches.replace(/\[([^\]]+)\]\([^)]+\)/g, '$1') + ' ' + (p.next || '');
  const widthFor = s => (s || '').length + 3 + 'ch';
  const ql = q.trim().toLowerCase();
  const toggleTag = k => {
    setStart(false);
    setSel(s => !ql && s === k ? null : k);
  };
  const clear = () => {
    setStart(false);
    setSel(null);
    setQ('');
  };
  const results = useMemo(() => {
    const list = PROMPTS.filter(p => {
      if (ql) return p.title.toLowerCase().includes(ql) || bodyText(p).toLowerCase().includes(ql);
      if (start) return !!p.startN;
      if (sel) return tagsOf(p).includes(sel);
      return true;
    });
    if (ql) return list;
    if (start) return list.sort((a, b) => a.startN - b.startN);
    if (sel) return list.sort((a, b) => (a.roles || []).length - (b.roles || []).length || (b.sdlc === 'operate') - (a.sdlc === 'operate'));
    return list;
  }, [PROMPTS, ql, start, sel]);
  const matchSnippet = p => {
    if (!ql || p.title.toLowerCase().includes(ql)) return null;
    const txt = bodyText(p);
    const at = txt.toLowerCase().indexOf(ql);
    if (at < 0) return null;
    const lo = Math.max(0, at - 30), hi = Math.min(txt.length, at + ql.length + 50);
    return [lo > 0 ? '…' : '', txt.slice(lo, at), <mark key="m">{txt.slice(at, at + ql.length)}</mark>, txt.slice(at + ql.length, hi), hi < txt.length ? '…' : ''];
  };
  const grouped = useMemo(() => {
    if (start && !q.trim()) return [];
    const g = {};
    for (const p of results) {
      const key = p.sdlc + '|' + p.cat;
      (g[key] = g[key] || ({
        sdlc: p.sdlc,
        cat: p.cat,
        items: []
      })).items.push(p);
    }
    return Object.values(g);
  }, [results, start, q]);
  const copy = async (str, id) => {
    try {
      await navigator.clipboard.writeText(str);
    } catch {
      const ta = document.createElement('textarea');
      ta.value = str;
      ta.setAttribute('readonly', '');
      ta.style.position = 'fixed';
      ta.style.opacity = '0';
      document.body.appendChild(ta);
      ta.select();
      document.execCommand('copy');
      document.body.removeChild(ta);
    }
    clearTimeout(copyTimer.current);
    setCopied(id);
    copyTimer.current = setTimeout(() => setCopied(null), 1600);
  };
  const promptBody = p => {
    if (!p.slots) return <code>{p.prompt}</code>;
    const parts = p.prompt.split(/(\{\w+\})/g);
    return <code>
        {parts.map((part, idx) => {
      const m = part.match(/^\{(\w+)\}$/);
      if (!m) return <span key={idx}>{part}</span>;
      const k = m[1];
      const val = fillOf(p, k);
      return <input key={idx} type="text" className="pl-slot" value={val} placeholder={p.slots[k] || k} aria-label={k} style={{
        width: widthFor(val || p.slots[k])
      }} onChange={e => setFill(p.id, k, e.target.value)} onFocus={e => e.target.select()} onClick={e => e.stopPropagation()} />;
    })}
      </code>;
  };
  const card = p => {
    const open = openId === p.id;
    const srcHref = SOURCES[p.src];
    const srcLabel = sourceLabels[p.src];
    const snip = matchSnippet(p);
    return <div key={p.id} className={'pl-card' + (open ? ' pl-open' : '')}>
        <button type="button" className="pl-head" onClick={() => setOpenId(open ? null : p.id)} aria-expanded={open}>
          <span className="pl-title">{p.title}</span>
          {!!p.startN && <span className="pl-chip">{L.startHere} · {p.startN}</span>}
        </button>
        {snip ? <div className="pl-match">{snip}</div> : <code className="pl-prompt-preview">{preview(p)}</code>}
        {open && <div className="pl-body">
            <div className="pl-label">{p.slots ? L.fillAndCopy : L.copyThis}</div>
            {p.needs && L.needs && L.needs[p.needs] && <div className="pl-hint pl-needs">
                <span className="pl-needs-label">{L.needsLabel}</span> {linkify(L.needs[p.needs])}
              </div>}
            {p.paste && L.paste && L.paste[p.paste] && <div className="pl-hint pl-paste">{L.paste[p.paste]}</div>}
            {p.slots && <div className="pl-hint">
                {L.hintBefore} <span className="pl-hint-chip">{L.hintChip}</span> {L.hintAfter}
              </div>}
            <div className="pl-prompt-box">
              <span className="pl-caret">{'❯'}</span>
              {promptBody(p)}
              <button type="button" className="pl-copy" onClick={() => copy(assemble(p), p.id)}>
                {copied === p.id ? L.copied : L.copy}
              </button>
            </div>
            <div className="pl-label">{L.whyWorks}</div>
            <div className="pl-teaches">{linkify(p.teaches)}</div>
            {p.nextHref && p.next && <div className="pl-next">
                <span className="pl-next-label">{L.makeItStick}</span>
                <a href={doc(p.nextHref)}>{codeify(p.next)} →</a>
              </div>}
            {srcLabel && <div className="pl-src">{L.from} {srcHref ? <a href={doc(srcHref)}>{srcLabel}</a> : srcLabel}</div>}
          </div>}
      </div>;
  };
  const STYLES = useMemo(() => `
.pl {
  --pl-accent: #D97757;
  --pl-accent-bg: rgba(217,119,87,0.07);
  --pl-bg: #fff;
  --pl-surface: #FAFAF7;
  --pl-border: #E8E6DC;
  --pl-border-subtle: rgba(31,30,29,0.08);
  --pl-text: #141413;
  --pl-text-2: #5E5D59;
  --pl-text-3: #73726C;
  --pl-text-4: #9C9A92;
  --pl-mono: var(--font-mono, ui-monospace, SFMono-Regular, Menlo, monospace);
  font-family: 'Anthropic Sans', -apple-system, BlinkMacSystemFont, sans-serif;
  font-size: 16px; color: var(--pl-text); margin: 8px 0 32px;
}
.dark .pl {
  --pl-bg: #1f1e1d;
  --pl-surface: #262624;
  --pl-border: #3d3d3a;
  --pl-border-subtle: rgba(240,238,230,0.08);
  --pl-text: #f0eee6;
  --pl-text-2: #bfbdb4;
  --pl-text-3: #91908a;
  --pl-text-4: #73726c;
}
.pl *, .pl *::before, .pl *::after { box-sizing: border-box; }
.pl button { font-family: inherit; cursor: pointer; }
.pl a { color: var(--pl-accent); text-decoration: none; }
.pl a:hover { text-decoration: underline; }

.pl-search {
  display: flex; align-items: center; gap: 10px;
  padding: 14px 18px; background: var(--pl-surface);
  border: 1px solid var(--pl-border); border-radius: 12px;
  margin-bottom: 14px;
}
.pl-search input {
  flex: 1; border: none; outline: none; background: transparent;
  font-size: 16px; color: var(--pl-text);
}
.pl-search input::placeholder { color: var(--pl-text-4); }

.pl-tags { display: flex; gap: 8px; flex-wrap: wrap; align-items: center; margin-bottom: 18px; }
.pl-tag {
  padding: 7px 14px; border: 1px solid var(--pl-border); background: var(--pl-bg);
  font-size: 14px; color: var(--pl-text-2); border-radius: 999px;
}
.pl-tag:hover { background: var(--pl-surface); }
.pl-tag.pl-on { background: var(--pl-text); border-color: var(--pl-text); color: var(--pl-bg); }
.pl-tag.pl-start { color: var(--pl-accent); font-weight: 500; }
.pl-tag.pl-start.pl-on { background: var(--pl-accent); border-color: var(--pl-accent); color: #fff; }
.pl-tags.pl-dim .pl-tag { opacity: 0.5; }
.pl-tags.pl-dim .pl-tag:hover { opacity: 1; }
.pl-sep { width: 1px; height: 22px; background: var(--pl-border); margin: 0 4px; }
.pl-clear { border: none; background: none; font-size: 13px; color: var(--pl-text-4); padding: 4px 6px; }
.pl-clear:hover { color: var(--pl-text-2); }
.pl-count { margin-left: auto; font-size: 14px; color: var(--pl-text-4); }

.pl-group-h {
  font-size: 12px; letter-spacing: 0.08em; text-transform: uppercase;
  color: var(--pl-text-4); margin: 24px 0 12px;
}
.pl-group-h .pl-phase { color: var(--pl-text-3); }
.pl-card {
  border: 1px solid var(--pl-border-subtle); border-radius: 10px;
  margin-bottom: 12px; background: var(--pl-bg); overflow: hidden;
  padding: 14px 18px;
}
.pl-card.pl-open { border-color: var(--pl-border); background: var(--pl-surface); }
.pl-head {
  width: 100%; display: flex; align-items: baseline; gap: 12px;
  border: none; background: transparent; text-align: left; padding: 0;
}
.pl-head:focus-visible { outline: 2px solid var(--pl-accent); outline-offset: 2px; border-radius: 6px; }
.pl-title {
  flex: 1; font-size: 17px; font-weight: 500; color: var(--pl-text);
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}
.pl-prompt-preview {
  display: block; font-family: var(--pl-mono); font-size: 13.5px; color: var(--pl-text-3);
  margin-top: 6px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}
.pl-chip {
  font-size: 11px; letter-spacing: 0.05em; text-transform: uppercase;
  padding: 3px 9px; border-radius: 999px; flex-shrink: 0;
  background: var(--pl-accent-bg); color: var(--pl-accent);
}

.pl-body { margin-top: 14px; padding-top: 14px; border-top: 1px solid var(--pl-border-subtle); }
.pl-label {
  font-size: 11.5px; letter-spacing: 0.08em; text-transform: uppercase;
  color: var(--pl-text-4); margin: 12px 0 8px;
}
.pl-prompt-box {
  display: flex; align-items: center; gap: 10px;
  padding: 14px 16px; background: #141413; color: #f0eee6;
  border-radius: 8px; font-family: var(--pl-mono); font-size: 15px;
}
.pl-caret { color: var(--pl-accent); flex-shrink: 0; }
.pl-prompt-box code { flex: 1; background: none; padding: 0; color: inherit; white-space: pre-wrap; line-height: 1.9; }
.pl-slot {
  font-family: var(--pl-mono); font-size: inherit;
  background: rgba(217,119,87,0.15); color: #f0eee6;
  border: none; border-bottom: 1.5px dashed var(--pl-accent);
  border-radius: 4px 4px 0 0; padding: 2px 6px; margin: 0 1px;
  outline: none; min-width: 6ch; max-width: 100%;
  box-sizing: content-box; cursor: text;
}
.pl-slot:hover { background: rgba(217,119,87,0.22); }
.pl-slot:focus { background: rgba(217,119,87,0.28); border-bottom-style: solid; }
.pl-slot::placeholder { color: rgba(240,238,230,0.4); font-style: italic; }
.pl-hint { font-size: 14px; color: var(--pl-text-3); margin: 0 0 10px; }
.pl-paste { color: var(--pl-text-2); }
.pl-needs { color: var(--pl-text-2); }
.pl-needs-label {
  display: inline-block; font-size: 10.5px; letter-spacing: 0.06em;
  text-transform: uppercase; padding: 2px 7px; margin-right: 6px;
  border-radius: 4px; background: var(--pl-accent-bg); color: var(--pl-accent);
}
.pl-hint-chip {
  font-family: var(--pl-mono); font-size: 0.92em;
  background: var(--pl-accent-bg); color: var(--pl-accent);
  border-bottom: 1.5px dashed var(--pl-accent);
  border-radius: 3px 3px 0 0; padding: 1px 5px;
}
.pl-copy {
  font-size: 12.5px; padding: 6px 12px; border-radius: 6px;
  background: var(--pl-accent); color: #fff; border: none; flex-shrink: 0;
}
.pl-teaches { display: block; font-size: 15.5px; color: var(--pl-text-2); margin: 4px 0 0; line-height: 1.6; }
.pl-match {
  display: block; font-size: 13.5px; color: var(--pl-text-3);
  margin-top: 6px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}
.pl-match mark { background: var(--pl-accent-bg); color: var(--pl-text); padding: 1px 2px; border-radius: 3px; }
.pl-next {
  display: flex; align-items: baseline; gap: 10px;
  margin: 14px 0 0; padding: 10px 12px;
  background: var(--pl-accent-bg); border-radius: 8px; font-size: 14.5px;
}
.pl-next-label {
  font-size: 11px; letter-spacing: 0.06em; text-transform: uppercase;
  color: var(--pl-accent); font-weight: 600; flex-shrink: 0;
}
.pl-src { display: block; font-size: 14px; color: var(--pl-text-4); margin: 14px 0 0; }

.pl-show-all {
  display: block; width: 100%; padding: 14px; margin-top: 4px;
  border: 1px dashed var(--pl-border); border-radius: 10px;
  background: transparent; font-size: 15px; color: var(--pl-accent);
  text-align: center;
}
.pl-show-all:hover { background: var(--pl-accent-bg); border-style: solid; }

.pl-empty {
  padding: 32px; text-align: center; color: var(--pl-text-4);
  border: 1px dashed var(--pl-border); border-radius: 10px;
}
`, []);
  if (!mounted) return <div className="pl" style={{
    minHeight: 480
  }} />;
  return <div className="pl">
      <style>{STYLES}</style>

      <div className="pl-search">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" style={{
    color: 'var(--pl-text-4)'
  }}>
          <circle cx="11" cy="11" r="7" /><line x1="21" y1="21" x2="16.65" y2="16.65" />
        </svg>
        <input type="text" placeholder={L.search} value={q} onChange={e => {
    setQ(e.target.value);
    if (e.target.value) setStart(false);
  }} aria-label={L.search} />
      </div>

      <div className={'pl-tags' + (ql ? ' pl-dim' : '')}>
        <button type="button" className={'pl-tag pl-start' + (!ql && start ? ' pl-on' : '')} onClick={() => {
    setQ('');
    setStart(!start);
    if (!start) setSel(null);
  }}>
          ★ {L.startHere}
        </button>
        <span className="pl-sep" />
        {TAGS.map(k => <button key={k} type="button" aria-pressed={!ql && sel === k} className={'pl-tag' + (!ql && sel === k ? ' pl-on' : '')} onClick={() => {
    setQ('');
    toggleTag(k);
  }}>
            {TL(k)}
          </button>)}
        {(start || sel || q) && <button type="button" className="pl-clear" onClick={clear}>{L.clear}</button>}
        <span className="pl-count">{results.length} {results.length === 1 ? L.prompt : L.prompts}</span>
      </div>

      {results.length === 0 ? <div className="pl-empty">
          {L.noMatch} {ql ? <code>{q}</code> : null} <button type="button" className="pl-clear" onClick={clear}>{L.clear}</button>
        </div> : !ql && start ? <div>
          <div className="pl-group-h">{L.startHereHeader}</div>
          {results.map(card)}
          <button type="button" className="pl-show-all" onClick={clear}>
            {L.showAll && L.showAll.replace('{n}', PROMPTS.length)} →
          </button>
        </div> : grouped.map(g => <div key={g.sdlc + '|' + g.cat}>
            <div className="pl-group-h"><span className="pl-phase">{phaseLabels[g.sdlc] || g.sdlc}</span> · {catLabels[g.cat] || g.cat}</div>
            {g.items.map(card)}
          </div>)}
    </div>;
};

Esta é uma biblioteca de prompts para copiar no Claude Code. Use-a para explorar formas de trabalho que você ainda não experimentou, ou quando não tem certeza por onde começar.

Os prompts foram coletados de vários guias da Anthropic, incluindo [Fluxos de trabalho comuns](https://code.claude.com/docs/pt/common-workflows), [Melhores práticas](https://code.claude.com/docs/pt/best-practices) e [Como os times da Anthropic usam Claude Code](https://claude.com/blog/how-anthropic-teams-use-claude-code). Eles são pontos de partida, não scripts. Abra **Por que isso funciona** em qualquer prompt para ver o padrão por trás dele, para que você possa escrever o seu próprio.

export const labels = {
  startHere: "Comece aqui",
  startHereHeader: "Cinco prompts para tentar primeiro",
  showAll: "Mostrar todos os {n} prompts",
  search: "Pesquisar prompts…",
  clear: "Limpar",
  prompt: "prompt",
  prompts: "prompts",
  noMatch: "Nenhum prompt corresponde",
  fillAndCopy: "Preencher e copiar",
  copyThis: "Copiar este prompt",
  hintBefore: "Digite nos",
  hintChip: "campos destacados",
  hintAfter: "para personalizar e depois copie.",
  copy: "Copiar",
  copied: "Copiado",
  whyWorks: "Por que isso funciona",
  makeItStick: "Fixe isso",
  from: "De",
  paste: {
    mockup: "Cole, arraste ou @-mencione sua imagem de mockup e depois envie isto:",
    design: "Cole, arraste ou @-mencione sua imagem de design e depois envie isto:",
    screenshot: "Cole, arraste ou @-mencione sua captura de tela e depois envie isto:",
    plan: "Cole sua saída de plano no prompt primeiro e depois envie isto:",
    error: "Cole a saída de erro no prompt primeiro e depois envie isto:",
    csv: "Arraste seu arquivo para o prompt ou substitua o caminho abaixo por uma @-menção do seu:"
  },
  needsLabel: "Precisa",
  needs: {
    tracker: "seu rastreador de problemas adicionado como um [conector claude.ai](https://code.claude.com/docs/pt/mcp#use-mcp-servers-from-claude-ai) ou [servidor MCP](https://code.claude.com/docs/pt/mcp).",
    gh: "a [CLI gh](https://cli.github.com) autenticada, ou GitHub adicionado como um [conector claude.ai](https://code.claude.com/docs/pt/mcp#use-mcp-servers-from-claude-ai).",
    browser: "uma forma para Claude renderizar e fazer captura de tela do resultado. O [aplicativo Desktop](https://code.claude.com/docs/pt/desktop#preview-your-app) tem isso integrado. No terminal, instale a [extensão Chrome](https://code.claude.com/docs/pt/chrome) ou um servidor MCP [Playwright](https://code.claude.com/docs/pt/mcp).",
    db: "seu data warehouse ou armazenamento de logs adicionado como um [conector claude.ai](https://code.claude.com/docs/pt/mcp#use-mcp-servers-from-claude-ai) ou [servidor MCP](https://code.claude.com/docs/pt/mcp)."
  }
};

export const tagLabels = {
  understand: "Entender",
  plan: "Planejar",
  prototype: "Protótipo",
  build: "Construir",
  test: "Testar",
  refactor: "Refatorar",
  review: "Revisar",
  steer: "Orientar",
  debug: "Depurar",
  git: "Git",
  release: "Lançamento",
  data: "Dados",
  automate: "Automatizar",
  pm: "Produto",
  design: "Design",
  docs: "Documentação",
  marketing: "Marketing",
  security: "Segurança",
  ops: "Plantão"
};

export const phaseLabels = {
  discover: "Descobrir",
  design: "Design",
  build: "Construir",
  ship: "Lançar",
  operate: "Operar"
};

export const sourceLabels = {
  workflows: "Fluxos de trabalho comuns",
  teams: "Como os times da Anthropic usam Claude Code",
  legal: "Como a Anthropic usa Claude em Jurídico",
  cybersecurity: "Como a Anthropic usa Claude em Segurança Cibernética",
  "best-practices": "Melhores práticas",
  ebook: "Guia de codificação agentic em escala"
};

export const catLabels = {
  Onboard: "Integração",
  Understand: "Entender",
  Plan: "Planejar",
  Prototype: "Protótipo",
  Implement: "Implementar",
  Test: "Testar",
  Refactor: "Refatorar",
  Review: "Revisar",
  Steer: "Orientar",
  Git: "Git",
  Release: "Lançamento",
  Debug: "Depurar",
  Incident: "Incidente",
  Data: "Dados",
  Automate: "Automatizar"
};

export const text = {
  "get-oriented-in-a": {
    title: "Orientar-se em um novo repositório",
    teaches: "Descreva o que você quer saber, não quais arquivos ler. Claude explora o projeto por conta própria e retorna um resumo de como ele se encaixa.",
    next: "Execute `/init` para configurar `CLAUDE.md` para que Claude se lembre disso a cada sessão"
  },
  "explain-unfamiliar-code": {
    title: "Explicar código desconhecido",
    teaches: "Nomeie o arquivo e diga em qual formato você quer a resposta. Troque a página HTML por um diagrama, pontos de bala ou o que se adequar a como você aprende.",
    next: "Defina um estilo de saída para que Claude sempre explique em seu formato preferido"
  },
  "find-where-something-happens": {
    title: "Encontrar onde algo acontece",
    teaches: "Pesquise por comportamento em vez de por nome de arquivo. A pesquisa funciona mesmo quando você não sabe como o arquivo é chamado ou em qual diretório ele está."
  },
  "see-what-depends-on": {
    title: "Verificar o que quebra antes de você deletar",
    teaches: "Pergunte antes de remover qualquer coisa. A lista de chamadores e efeitos downstream diz se você está olhando para uma limpeza de uma linha ou uma mudança que você precisa coordenar."
  },
  "trace-how-code-evolved": {
    title: "Rastrear como o código evoluiu",
    teaches: "Aponte para o histórico de commits quando a pergunta é por quê, não o quê. Claude lê o log e blame para qualquer controle de versão que você use e explica as decisões por trás da implementação atual."
  },
  "scope-a-change-before": {
    title: "Definir o escopo de uma mudança antes de começar",
    teaches: "Dimensione o trabalho antes de comprometê-lo com um roadmap. A lista de arquivos diz se você está olhando para um componente ou uma mudança transversal."
  },
  "ask-the-codebase-a": {
    title: "Fazer uma pergunta de produto à base de código",
    teaches: "Declare seu papel para que a resposta seja no nível certo. Claude explica o que o produto realmente faz a partir do código-fonte, sem você precisar lê-lo.",
    next: "Defina um estilo de saída para que Claude sempre apresente respostas neste nível"
  },
  "plan-a-multi-file": {
    title: "Planejar uma mudança em vários arquivos antes de tocar no código",
    teaches: "Adicionar \"não edite ainda\" separa exploração de mudanças, para que você veja a abordagem antes de qualquer código se mover. Para fazer o planejamento primeiro o padrão em cada prompt, pressione Shift+Tab para [modo de plano](https://code.claude.com/docs/pt/permission-modes#analyze-before-you-edit-with-plan-mode)."
  },
  "draft-a-spec-by": {
    title: "Rascunhar uma especificação por entrevista",
    teaches: "Peça para ser entrevistado em vez de escrever a especificação você mesmo. Claude faz perguntas estruturadas até que os requisitos estejam completos e depois escreve o resultado em um arquivo.",
    next: "Salve suas perguntas de entrevista como uma skill `/spec` para que cada especificação comece da mesma forma"
  },
  "turn-a-meeting-into": {
    title: "Transformar uma reunião em tickets",
    teaches: "Pule a etapa de transcrição. Claude extrai itens de ação da entrada não estruturada e os escreve diretamente em seu rastreador via [MCP](https://code.claude.com/docs/pt/mcp), para que você revise os tickets, não a transcrição.",
    next: "Salve isto como uma skill `/tickets`"
  },
  "map-edge-cases-before": {
    title: "Mapear casos extremos antes de construir",
    teaches: "Peça pelo que está faltando, não pelo que está lá. Claude lista os estados de erro, estados vazios e casos extremos que um design de caminho feliz tende a pular."
  },
  "turn-a-mockup-into": {
    title: "Transformar um mockup em um protótipo funcional",
    teaches: "Um protótipo clicável responde perguntas que um mockup estático não consegue. Entregue o código funcional à engenharia em vez de explicar as interações em um documento."
  },
  "implement-from-a-screenshot": {
    title: "Implementar a partir de uma captura de tela e auto-verificar",
    teaches: "Isto dá a Claude um loop de verificação: ele renderiza, compara com a imagem de origem e itera sem você apontar cada lacuna.",
    next: "Use `/goal` para manter Claude iterando até que as capturas de tela correspondam"
  },
  "follow-an-existing-pattern": {
    title: "Seguir um padrão existente",
    teaches: "Aponte para código que você já gosta. Sem uma referência, Claude usa como padrão as melhores práticas gerais. Com uma, ele corresponde às convenções que sua base de código realmente usa.",
    next: "Peça a Claude para escrever o padrão que seguiu em `CLAUDE.md` para que futuras sessões o correspondam sem a referência"
  },
  "add-a-small-well": {
    title: "Adicionar um recurso pequeno e bem definido",
    teaches: "Declare as entradas e saídas, não como construir. Claude encontra onde código similar vive e adiciona o seu ao lado."
  },
  "build-a-small-internal": {
    title: "Construir uma pequena ferramenta interna do zero",
    teaches: "Você não precisa de um projeto, um framework ou uma etapa de construção. Descreva a ferramenta e peça a Claude para abri-la para que você a veja funcionando imediatamente."
  },
  "work-an-issue-end": {
    title: "Trabalhar um problema de ponta a ponta",
    teaches: "Dê o número do problema, não um resumo. Claude lê o ticket completo em si, para que requisitos que você esqueceria de mencionar apareçam e valide a mudança antes de relatar."
  },
  "find-and-update-copy": {
    title: "Encontrar e atualizar cópia em toda a base de código",
    teaches: "Peça por variantes e diga o que pular. Claude encontra fraseados que uma pesquisa literal perderia e deixa fixtures de teste e histórico intocados, para que você revise apenas a cópia que os usuários realmente veem."
  },
  "draft-from-past-examples": {
    title: "Rascunhar um documento a partir de exemplos passados",
    teaches: "Aponte para uma pasta de trabalho concluída em vez de descrever seu estilo. Claude aprende a estrutura e a voz do que você já lançou, para que o primeiro rascunho pareça um dos seus.",
    next: "Salve a voz como uma skill para que cada rascunho comece lá"
  },
  "write-tests-run-them": {
    title: "Escrever testes, executá-los, corrigir falhas",
    teaches: "Peça para escrever, executar e corrigir juntos para que Claude itere sem parar para instruções.",
    next: "Execute `/init` para que Claude aprenda seu comando de teste automaticamente"
  },
  "drive-implementation-from-tests": {
    title: "Conduzir implementação a partir de testes",
    teaches: "Desenvolvimento orientado por testes: os testes definem quando o trabalho está completo e Claude itera na implementação até que passem."
  },
  "fill-gaps-from-a": {
    title: "Preencher lacunas a partir de um relatório de cobertura",
    teaches: "Aponte para o relatório de cobertura em vez de adivinhar o que não foi testado. Claude lê os números reais e escreve testes para os arquivos que mais precisam.",
    next: "Defina isto como um `/goal` para que Claude continue escrevendo testes até que a cobertura atinja o alvo"
  },
  "port-code-between-languages": {
    title: "Portar código para outra linguagem",
    teaches: "Diga o que preservar, não apenas a linguagem de destino. Nomear a API ou comportamento que deve permanecer igual dá a Claude um contrato para verificar a porta."
  },
  "generate-docs-for-code": {
    title: "Gerar documentação para código não documentado",
    teaches: "Nomeie o escopo e o formato. Claude encontra o que está faltando e corresponde ao estilo de comentário já no arquivo, para que a nova documentação pareça o resto."
  },
  "migrate-a-pattern-across": {
    title: "Migrar um padrão em toda a base de código",
    teaches: "Descreva o padrão antigo e o novo. Pedir a Claude para identificar cada lugar primeiro significa que os sites de chamada são listados na resposta, para que você possa verificar se nenhum foi perdido."
  },
  "optimize-against-a-measurable": {
    title: "Otimizar contra um alvo mensurável",
    teaches: "Declarar a métrica e o alvo dá a Claude uma definição clara de conclusão.",
    next: "Defina isto como um `/goal` para que Claude continue medindo e iterando até atingir o número"
  },
  "fix-a-precise-visual": {
    title: "Corrigir um bug visual preciso",
    teaches: "Feedback visual preciso obtém uma correção precisa. Declare o elemento exato, medição e viewport.",
    next: "Adicione uma ferramenta de visualização para que Claude faça captura de tela e verifique a correção em si"
  },
  "review-your-changes-before": {
    title: "Revisar suas mudanças antes de fazer commit",
    teaches: "Pegue problemas enquanto ainda são baratos de corrigir. Claude lê os arquivos alterados na íntegra, não apenas as linhas de diff, para que detecte problemas que uma auto-revisão rápida perde.",
    next: "Execute `/code-review` para a mesma verificação em um comando"
  },
  "review-a-pull-request": {
    title: "Revisar um pull request",
    teaches: "Claude revisa com toda a base de código em contexto, não apenas o diff. Ele lê o código alterado e o que ele chama, para que detecte problemas que uma revisão apenas de diff perderia.",
    next: "Ative isto para cada PR com Code Review"
  },
  "review-infrastructure-changes-before": {
    title: "Revisar mudanças de infraestrutura antes de aplicar",
    teaches: "A saída do plano é densa e difícil de escanear. Colá-la obtém um resumo em linguagem simples do que realmente vai mudar antes de você aplicar."
  },
  "run-a-security-review": {
    title: "Executar uma revisão de segurança com um subagente",
    teaches: "Um [subagente](https://code.claude.com/docs/pt/sub-agents) executa a auditoria em sua própria janela de contexto e relata um resumo, para que uma revisão de segurança longa não preencha sua sessão principal. O subagente de propósito geral integrado lida com isto sem configuração extra.",
    next: "Configure um subagente dedicado de revisão de segurança que todo o seu time possa usar"
  },
  "review-content-before-sending": {
    title: "Detectar problemas antes da revisão formal",
    teaches: "Obtenha uma primeira passagem antes de um humano gastar tempo com isto. Nomeie as preocupações que você quer verificadas para que a revisão seja focada e depois corrija o que encontra e envie um rascunho mais limpo.",
    next: "Capture sua lista de verificação de revisão como uma skill que todo o seu time possa executar"
  },
  "course-correct-a-wrong": {
    title: "Corrigir uma abordagem errada",
    teaches: "Nomeie a restrição que Claude perdeu, não apenas que está errado. Uma razão específica dá a Claude uma restrição concreta a satisfazer na tentativa novamente, em vez de adivinhar novamente.",
    next: "Pressione `Esc` duas vezes para abrir o menu de retrocesso e restaurar código e conversa para que a tentativa novamente comece limpa"
  },
  "narrow-the-scope-of": {
    title: "Estreitar o escopo de uma mudança",
    teaches: "Quando a direção está certa mas a mudança ficou muito ampla, peça a Claude para manter parte dela em vez de retroceder tudo. Um limite declarado mantém uma pequena correção de se tornar uma refatoração."
  },
  "turn-a-correction-into": {
    title: "Transformar uma correção em uma regra",
    teaches: "Uma correção no chat não é compartilhada com seu time. Uma regra no [CLAUDE.md](https://code.claude.com/docs/pt/memory) do projeto é compartilhada uma vez que você a faz commit e Claude a lê no início de cada sessão.",
    next: "Abra `/memory` para revisar o que Claude escreveu"
  },
  "resolve-merge-conflicts": {
    title: "Resolver conflitos de merge",
    teaches: "Diga qual estado você quer, não quais marcadores manter. Pedir o raciocínio torna o merge revisável em vez de uma caixa preta."
  },
  "commit-with-a-generated": {
    title: "Fazer commit com uma mensagem gerada",
    teaches: "Deixe Claude derivar a mensagem do diff. Ela corresponde ao estilo de commit existente do seu repositório."
  },
  "open-a-pull-request": {
    title: "Abrir um pull request a partir de um ticket",
    teaches: "Pule a troca de contexto entre rastreador, editor e GitHub. Um prompt lê a especificação, faz a mudança e abre o PR."
  },
  "draft-release-notes-from": {
    title: "Rascunhar notas de lançamento do histórico git",
    teaches: "Dê dois pontos de referência e a estrutura que você quer. Claude lê o log de commit entre eles e rascunha um changelog que você pode editar.",
    next: "Salve isto como uma skill `/changelog`"
  },
  "write-a-ci-workflow": {
    title: "Escrever um workflow de CI",
    teaches: "Descreva quando deve ser executado e o que deve fazer; o YAML é gerado para você, correspondido aos comandos de construção e teste do seu projeto."
  },
  "find-and-fix-a": {
    title: "Encontrar e corrigir um teste falhando",
    teaches: "Descreva o sintoma; você não precisa saber qual arquivo está quebrado. Claude executa o teste para ver a falha, rastreia-a para a origem e a corrige."
  },
  "investigate-a-reported-error": {
    title: "Investigar um erro relatado",
    teaches: "Descreva o sintoma e localização; Claude lê o caminho de código relevante e rastreia as causas prováveis. Cole stack traces ou logs se você tiver.",
    next: "Coloque um deeplink em seu runbook que abre Claude com este prompt pré-preenchido"
  },
  "fix-a-build-error": {
    title: "Corrigir um erro de construção na raiz",
    teaches: "Pedir causa raiz e verificação previne patches de nível de superfície que suprimem o erro sem corrigi-lo."
  },
  "investigate-a-production-incident": {
    title: "Investigar um incidente de produção",
    teaches: "Liste as fontes de evidência para correlacionar, não os passos a tomar. Claude lê logs, histórico git e config juntos para estreitar a causa.",
    next: "Conecte Sentry ou seu armazenamento de logs via MCP"
  },
  "query-logs-in-plain": {
    title: "Consultar logs em inglês simples",
    teaches: "Faça a pergunta em vez de escrever o SQL. Claude constrói a consulta, a executa contra seus logs conectados e mostra tanto a consulta quanto o resultado para que você possa verificar o que foi executado."
  },
  "diagnose-from-a-console": {
    title: "Diagnosticar a partir de uma captura de tela do console",
    teaches: "Consoles em nuvem mostram o problema mas não os comandos para corrigi-lo. Claude lê a captura de tela e traduz o dashboard nos comandos kubectl, gcloud ou aws para executar."
  },
  "analyze-a-data-file": {
    title: "Analisar um arquivo de dados",
    teaches: "Uma pergunta única não precisa de um script único. Aponte para um arquivo em sua pasta de projeto e Claude o lê diretamente, encontra os padrões e escreve a saída onde você pedir.",
    next: "Conecte a fonte de dados via MCP em vez de exportar arquivos"
  },
  "generate-variations-from-performance": {
    title: "Gerar variações a partir de dados de desempenho",
    teaches: "Declare a restrição no início para que a geração permaneça dentro do limite. Claude lê as métricas, escolhe o que substituir e produz alternativas que se encaixam.",
    next: "Conecte a plataforma de anúncios via MCP em vez de exportar um arquivo"
  },
  "turn-a-recurring-task": {
    title: "Transformar uma tarefa recorrente em uma skill",
    teaches: "Nomeie os passos uma vez; reutilize-os como um comando. Claude escreve uma [skill](https://code.claude.com/docs/pt/skills) que qualquer pessoa do seu time possa executar."
  },
  "add-a-hook-for": {
    title: "Adicionar um hook para comportamento repetido",
    teaches: "Hooks tornam um comportamento automático em vez de algo que você tem que se lembrar de pedir. Descreva o gatilho e ação e Claude escreve a configuração do [hook](https://code.claude.com/docs/pt/hooks)."
  },
  "connect-a-tool-with": {
    title: "Conectar uma ferramenta com MCP",
    teaches: "Conecte a fonte uma vez em vez de colar dados a cada sessão. Após a configuração do [MCP](https://code.claude.com/docs/pt/mcp), Claude lê da ferramenta diretamente quando você pergunta sobre ela."
  },
  "capture-what-to-remember": {
    title: "Capturar o que lembrar para a próxima vez",
    teaches: "Pergunte antes de esquecer. Claude sabe o que teve que descobrir nesta sessão e propõe entradas [CLAUDE.md](https://code.claude.com/docs/pt/memory) para que a próxima sessão comece com esse contexto."
  }
};

<PromptLibrary text={text} labels={labels} tagLabels={tagLabels} phaseLabels={phaseLabels} sourceLabels={sourceLabels} catLabels={catLabels} />

<h2 id="what-makes-these-prompts-work">
  O que torna esses prompts funcionarem
</h2>

Os prompts acima compartilham alguns padrões. Reconhecê-los ajuda você a adaptar qualquer prompt aqui para sua própria tarefa.

**Descreva o resultado, não os passos.** Diga o que você quer e deixe Claude encontrar os arquivos. O prompt abaixo funciona sem nomear um único caminho de arquivo.

```text theme={null}
adicionar rate limiting à API pública e certificar-se de que os testes existentes ainda passam
```

**Dê a ele uma forma de verificar seu próprio trabalho.** Peça para executar, testar, comparar ou verificar no mesmo prompt para que Claude itere em vez de parar após uma tentativa.

```text theme={null}
escrever a migração, executá-la contra o banco de dados de desenvolvimento e confirmar que o schema corresponde
```

**Aponte para uma referência.** Nomeie um arquivo, teste ou padrão existente para corresponder para que o novo código seja consistente com o que você já tem.

```text theme={null}
adicionar uma página de configurações que segue o mesmo layout que a página de perfil
```

**Declare o alvo mensurável.** Quando o objetivo é desempenho ou cobertura, dê a métrica e limite para que a conclusão seja inequívoca.

```text theme={null}
obter o tamanho do bundle abaixo de 200KB e mostrar-me o que você removeu
```

**Dê a ele o artefato.** Cole erros, logs, capturas de tela e saída de plano diretamente no prompt ou digite `@` para referenciar um arquivo. Claude lê a origem em vez de sua descrição dela.

```text theme={null}
por que a construção está falhando? @build.log
```

**Diga como você quer a resposta.** Nomeie o formato, comprimento ou público para que a explicação se encaixe em como você a usará. Para tornar um formato o padrão para cada resposta, defina um [estilo de saída](https://code.claude.com/docs/pt/output-styles).

```text theme={null}
explicar como a lógica de retry de pagamento funciona como uma página HTML com um diagrama e depois abri-la no meu navegador
```

Para mais sobre cada padrão, veja [melhores práticas](https://code.claude.com/docs/pt/best-practices).

<h2 id="where-these-come-from">
  De onde vêm esses
</h2>

Esses prompts são baseados em padrões de recursos publicados da Anthropic. Cada card vincula à sua origem:

* [Fluxos de trabalho comuns](https://code.claude.com/docs/pt/common-workflows): guias passo a passo para as tarefas principais
* [Melhores práticas](https://code.claude.com/docs/pt/best-practices): padrões de prompting e configuração de projeto
* [Como os times da Anthropic usam Claude Code](https://claude.com/blog/how-anthropic-teams-use-claude-code): fluxos de trabalho reais de times de engenharia, produto, design e dados, com aprofundamentos em [jurídico](https://claude.com/blog/how-anthropic-uses-claude-legal), [marketing](https://claude.com/blog/how-anthropic-uses-claude-marketing) e [segurança cibernética](https://claude.com/blog/how-anthropic-uses-claude-cybersecurity)
* [Guia de codificação agentic em escala](https://resources.anthropic.com/hubfs/Scaling%20agentic%20coding%20across%20your%20organization.pdf): o guia de adoção empresarial

Para passo a passo em vídeo desses padrões, veja o curso gratuito [Claude Code in Action](https://anthropic.skilljar.com/claude-code-in-action) na Anthropic Academy.

<h2 id="related-resources">
  Recursos relacionados
</h2>

Os prompts nesta página são pontos de partida. Uma vez que um funciona para seu projeto, o próximo passo é torná-lo repetível: salve-o como uma [skill](https://code.claude.com/docs/pt/skills) para que qualquer pessoa do seu time possa executá-lo como um `/command` e registre as convenções que Claude aprendeu em [CLAUDE.md](https://code.claude.com/docs/pt/memory) para que cada sessão comece com esse contexto em vez de Claude reaprendê-lo. Para mudanças maiores ou mais arriscadas, [modo de plano](https://code.claude.com/docs/pt/permission-modes#analyze-before-you-edit-with-plan-mode) mostra a lista de arquivos antes de qualquer edição acontecer.

Se você está introduzindo Claude Code em um time, veja [administração](https://code.claude.com/docs/pt/admin-setup) para configurações gerenciadas e política, e [custos e uso](https://code.claude.com/docs/pt/costs) para como este trabalho é cobrado em seu plano.
