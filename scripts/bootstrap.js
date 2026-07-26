#!/usr/bin/env node

import fs from "node:fs/promises";

const TOKEN = process.env.STAR_HISTORY_TOKEN || process.env.GITHUB_TOKEN;
const REPOSITORY = process.env.GITHUB_REPOSITORY;
const HISTORY_FILE = "history.json";
const PAGE_SIZE = 100;
const MAX_RETRIES = 6;
const RETRYABLE_STATUS = new Set([429, 500, 502, 503, 504]);

if (!TOKEN) {
    console.error("❌ STAR_HISTORY_TOKEN 未设置");
    process.exit(1);
}

if (!REPOSITORY || !REPOSITORY.includes("/")) {
    console.error("❌ GITHUB_REPOSITORY 格式错误，应为 owner/repo");
    process.exit(1);
}

const [owner, repo] = REPOSITORY.split("/");

const headers = {
    Authorization: `Bearer ${TOKEN}`,
    Accept: "application/vnd.github.star+json",
    "User-Agent": "star-history-pro"
};

function sleep(ms) {
    return new Promise(resolve => setTimeout(resolve, ms));
}

function todayIsoDate() {
    return new Date().toISOString().substring(0, 10);
}

function toSortedDailyEntries(daily) {
    return [...daily.entries()]
        .sort((a, b) => a[0].localeCompare(b[0]))
        .map(([date, count]) => ({ date, count }));
}

function buildPointsFromDaily(daily) {
    let total = 0;
    return toSortedDailyEntries(daily).map(({ date, count }) => {
        total += count;
        return {
            date,
            stars: total
        };
    });
}

async function loadCheckpoint() {
    try {
        const raw = await fs.readFile(HISTORY_FILE, "utf8");
        const json = JSON.parse(raw);

        if (json.repository !== REPOSITORY) {
            throw new Error(
                `history.json 中 repository=${json.repository} 与当前 ${REPOSITORY} 不一致。`
            );
        }

        const nextPage = Number.isInteger(json._checkpoint?.nextPage)
            ? json._checkpoint.nextPage
            : 1;

        const dailyEntries = Array.isArray(json._checkpoint?.dailyCounts)
            ? json._checkpoint.dailyCounts
            : [];

        const daily = new Map();

        for (const item of dailyEntries) {
            if (!item || typeof item.date !== "string")
                continue;
            const count = Number(item.count) || 0;
            if (count > 0)
                daily.set(item.date, count);
        }

        return {
            nextPage,
            daily
        };
    } catch (err) {
        if (err && err.code === "ENOENT") {
            return {
                nextPage: 1,
                daily: new Map()
            };
        }

        throw err;
    }
}

async function saveCheckpoint(daily, nextPage) {
    const points = buildPointsFromDaily(daily);
    const payload = {
        repository: REPOSITORY,
        updated: todayIsoDate(),
        points,
        _checkpoint: {
            nextPage,
            dailyCounts: toSortedDailyEntries(daily)
        }
    };

    const tmpFile = `${HISTORY_FILE}.tmp`;
    await fs.writeFile(tmpFile, JSON.stringify(payload, null, 2), "utf8");
    await fs.rename(tmpFile, HISTORY_FILE);
}

async function saveFinalHistory(daily) {
    const points = buildPointsFromDaily(daily);
    const payload = {
        repository: REPOSITORY,
        updated: todayIsoDate(),
        points
    };

    const tmpFile = `${HISTORY_FILE}.tmp`;
    await fs.writeFile(tmpFile, JSON.stringify(payload, null, 2), "utf8");
    await fs.rename(tmpFile, HISTORY_FILE);
}

async function fetchPage(page) {
    const url =
        `https://api.github.com/repos/${owner}/${repo}/stargazers` +
        `?per_page=${PAGE_SIZE}&page=${page}`;

    for (let attempt = 1; attempt <= MAX_RETRIES; attempt++) {
        const res = await fetch(url, { headers });

        if (res.ok)
            return res.json();

        const text = await res.text();
        const retryable = RETRYABLE_STATUS.has(res.status);
        const isLastTry = attempt === MAX_RETRIES;

        if (!retryable || isLastTry) {
            throw new Error(`GitHub API Error ${res.status}\n${text}`);
        }

        let waitMs = Math.min(30000, 500 * Math.pow(2, attempt - 1));

        const resetHeader = res.headers.get("x-ratelimit-reset");
        if (resetHeader && (res.status === 403 || res.status === 429)) {
            const resetEpochMs = Number(resetHeader) * 1000;
            if (Number.isFinite(resetEpochMs)) {
                waitMs = Math.max(waitMs, resetEpochMs - Date.now() + 1000);
            }
        }

        console.warn(
            `GitHub API ${res.status}，第 ${attempt}/${MAX_RETRIES} 次重试，等待 ${Math.ceil(waitMs / 1000)}s...`
        );

        await sleep(waitMs);
    }

    throw new Error("Unexpected retry state");
}

function applyStarsToDaily(daily, stargazers) {
    for (const star of stargazers) {
        if (!star.starred_at) {
            throw new Error(
                "GitHub 当前未返回 starred_at，无法 Bootstrap 历史数据。"
            );
        }

        const date = star.starred_at.substring(0, 10);
        daily.set(date, (daily.get(date) || 0) + 1);
    }
}

async function fetchAndPersistFromCheckpoint() {
    const checkpoint = await loadCheckpoint();
    const daily = checkpoint.daily;
    let page = checkpoint.nextPage;
    let fetchedInThisRun = 0;

    if (page > 1) {
        console.log(`从断点继续：page ${page}`);
    }

    while (true) {
        console.log(`Fetching page ${page}...`);

        const data = await fetchPage(page);

        if (data.length === 0) {
            break;
        }

        applyStarsToDaily(daily, data);
        fetchedInThisRun += data.length;

        await saveCheckpoint(daily, page + 1);

        if (data.length < PAGE_SIZE) {
            break;
        }

        page++;
    }

    await saveFinalHistory(daily);

    return {
        fetchedInThisRun,
        totalStars: buildPointsFromDaily(daily).at(-1)?.stars || 0,
        totalDays: daily.size
    };
}

async function main() {

    console.log(`Repository : ${REPOSITORY}`);

    const result = await fetchAndPersistFromCheckpoint();

    console.log("✅ history.json 已生成");
    console.log(`本次新增抓取 ${result.fetchedInThisRun} 条`);
    console.log(`总 Star 数 ${result.totalStars}`);
    console.log(`共 ${result.totalDays} 天`);
}

main().catch(err => {

    console.error(err);

    process.exit(1);

});