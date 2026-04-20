const { chromium } = require('playwright');
const fs = require('fs');
const path = require('path');

const BASE_URL = process.env.CAPTURE_URL || 'http://127.0.0.1:5500';
const OUT_DIR = path.resolve(process.cwd(), 'captures');

async function ensureDir(dir) {
  await fs.promises.mkdir(dir, { recursive: true });
}

async function shot(page, name) {
  const file = path.join(OUT_DIR, name);
  await page.locator('.app-window').screenshot({ path: file });
  console.log('saved:', file);
}

async function seedRichData(page) {
  await page.evaluate(() => {
    // 상단 KPI 값 채우기
    document.getElementById('kpiWoCount').textContent = '12';
    document.getElementById('kpiInProgress').textContent = '4';
    document.getElementById('kpiGoodQty').textContent = '3,420';
    document.getElementById('kpiDefectRate').textContent = '1.8%';

    // 작업지시 목록 풍부화
    const woBody = document.querySelector('#woGrid tbody');
    woBody.innerHTML = `
      <tr><td>412</td><td>WO-20260420-001</td><td>PLANNED</td></tr>
      <tr class="selected"><td>411</td><td>WO-20260419-015</td><td>IN_PROGRESS</td></tr>
      <tr><td>410</td><td>WO-20260419-014</td><td>COMPLETED</td></tr>
      <tr><td>409</td><td>WO-20260419-013</td><td>COMPLETED</td></tr>
      <tr><td>408</td><td>WO-20260418-098</td><td>IN_PROGRESS</td></tr>
      <tr><td>407</td><td>WO-20260418-097</td><td>PLANNED</td></tr>
    `;

    // 생산실적 현황 풍부화
    const prodBody = document.querySelector('#prodGrid tbody');
    prodBody.innerHTML = `
      <tr><td>1</td><td>411</td><td>320</td><td>8</td><td>kim.op</td><td>2026-04-20 21:35</td></tr>
      <tr><td>2</td><td>408</td><td>290</td><td>3</td><td>lee.op</td><td>2026-04-20 21:20</td></tr>
      <tr><td>3</td><td>410</td><td>500</td><td>4</td><td>park.op</td><td>2026-04-20 20:42</td></tr>
      <tr><td>4</td><td>409</td><td>450</td><td>9</td><td>choi.op</td><td>2026-04-20 20:11</td></tr>
      <tr><td>5</td><td>411</td><td>210</td><td>2</td><td>kim.op</td><td>2026-04-20 19:58</td></tr>
    `;

    // 수불 로그 풍부화
    const logBody = document.querySelector('#logGrid tbody');
    logBody.innerHTML = `
      <tr><td>2026-04-20 21:35</td><td>1</td><td>+320</td><td>4,580</td><td>PRODUCTION_RESULT#9201</td></tr>
      <tr><td>2026-04-20 21:20</td><td>2</td><td>+290</td><td>2,130</td><td>PRODUCTION_RESULT#9200</td></tr>
      <tr><td>2026-04-20 20:42</td><td>1</td><td>+500</td><td>4,260</td><td>PRODUCTION_RESULT#9198</td></tr>
      <tr><td>2026-04-20 20:11</td><td>3</td><td>+450</td><td>1,940</td><td>PRODUCTION_RESULT#9193</td></tr>
      <tr><td>2026-04-20 19:58</td><td>1</td><td>+210</td><td>3,760</td><td>PRODUCTION_RESULT#9191</td></tr>
    `;

    // 우측 알림/결과 영역 풍부화
    document.getElementById('alertList').innerHTML = `
      <li>안전재고 미만 제품 1건 (제품ID: 4)</li>
      <li>불량률 1.8% (관리 기준 2.0% 이내)</li>
      <li>대기 작업지시 2건</li>
    `;

    document.getElementById('result').textContent = JSON.stringify({
      dashboard: 'READY',
      nowShift: 'NIGHT-A',
      workCenter: 'LINE-03',
      openOrders: 12,
      inProgress: 4,
      cumulativeGoodQty: 3420,
      defectRate: '1.8%'
    }, null, 2);
  });
}

async function run() {
  await ensureDir(OUT_DIR);

  const browser = await chromium.launch({ headless: true });
  const page = await browser.newPage({ viewport: { width: 1600, height: 1100 } });

  try {
    await page.goto(BASE_URL, { waitUntil: 'domcontentloaded', timeout: 60000 });
    await page.waitForSelector('.app-window', { timeout: 15000 });
    await page.waitForTimeout(900);

    await seedRichData(page);

    // 01. 메인 대시보드(값 많은 상태)
    await shot(page, '01-main-dashboard.png');

    // 02. 작업지시 입력 예시
    await page.fill('#workOrderForm [name="workOrderNo"]', 'WO-20260420-016');
    await page.fill('#workOrderForm [name="productId"]', '2');
    await page.fill('#workOrderForm [name="processId"]', '3');
    await page.fill('#workOrderForm [name="plannedQty"]', '1500');
    await page.fill('#workOrderForm [name="plannedDate"]', '2026-04-21');
    await page.evaluate(() => {
      if (typeof setStatus === 'function') setStatus('WORK ORDER INPUT READY');
      document.getElementById('result').textContent = JSON.stringify({
        flowStep: 'WORK_ORDER_CREATED',
        workOrderNo: 'WO-20260420-016',
        productId: 2,
        processId: 3,
        plannedQty: 1500,
        status: 'PLANNED'
      }, null, 2);
    });
    await shot(page, '02-workorder-created.png');

    // 03. 생산실적 등록 예시
    await page.getByRole('button', { name: '생산실적' }).click();
    await page.fill('#productionForm [name="workOrderId"]', '411');
    await page.fill('#productionForm [name="goodQty"]', '680');
    await page.fill('#productionForm [name="defectQty"]', '12');
    await page.fill('#productionForm [name="resultAt"]', '2026-04-20T22:03');
    await page.fill('#productionForm [name="operator"]', 'kim.op');
    await page.fill('#productionForm [name="memo"]', '2차 투입(라인 안정화)');
    await page.evaluate(() => {
      if (typeof setStatus === 'function') setStatus('PRODUCTION SAVED');
      document.getElementById('result').textContent = JSON.stringify({
        flowStep: 'PRODUCTION_RESULT_REGISTERED',
        workOrderId: 411,
        goodQty: 680,
        defectQty: 12,
        accumulatedProducedQty: 1412,
        workOrderStatus: 'IN_PROGRESS'
      }, null, 2);
    });
    await shot(page, '03-production-result.png');

    // 04. 재고 조회 예시
    await page.getByRole('button', { name: '재고조회' }).click();
    await page.fill('#inventoryForm [name="productId"]', '1');
    await page.evaluate(() => {
      const invTbody = document.querySelector('#invGrid tbody');
      invTbody.innerHTML = `
        <tr><td>P-1001 (완제품A)</td><td>4,580</td><td>1,000</td></tr>
        <tr><td>P-1002 (완제품B)</td><td>2,130</td><td>800</td></tr>
        <tr><td>P-1003 (완제품C)</td><td>1,940</td><td>900</td></tr>
      `;
      if (typeof setStatus === 'function') setStatus('INVENTORY LOADED');
      document.getElementById('result').textContent = JSON.stringify({
        flowStep: 'INVENTORY_INQUIRY',
        requestedProductId: 1,
        snapshotCount: 3,
        safetyRiskCount: 1
      }, null, 2);
    });
    await shot(page, '04-inventory-balance.png');

    // 05. 완료 상태 + 로그
    await page.getByRole('button', { name: '작업지시' }).click();
    await page.evaluate(() => {
      const tbody = document.querySelector('#woGrid tbody');
      tbody.innerHTML = `
        <tr><td>412</td><td>WO-20260420-001</td><td>PLANNED</td></tr>
        <tr><td>411</td><td>WO-20260419-015</td><td>IN_PROGRESS</td></tr>
        <tr class="selected"><td>410</td><td>WO-20260419-014</td><td>COMPLETED</td></tr>
        <tr><td>409</td><td>WO-20260419-013</td><td>COMPLETED</td></tr>
      `;
      if (typeof setStatus === 'function') setStatus('COMPLETED');
      document.getElementById('result').textContent = JSON.stringify({
        flowStep: 'FLOW_DONE',
        transitions: ['PLANNED', 'IN_PROGRESS', 'COMPLETED'],
        inventoryLogRef: 'PRODUCTION_RESULT#9201',
        auditTrace: 'capt-2nd-pass'
      }, null, 2);
    });
    await shot(page, '05-completed-and-log.png');

    // 06. 코드 도움(lookup) 모달 화면
    await page.click('#view-workorder .lookup-btn[data-lookup="product"]');
    await page.evaluate(() => {
      const body = document.querySelector('#lookupGrid tbody');
      body.innerHTML = `
        <tr data-id="1"><td>1</td><td>P-1001</td><td>완제품A</td></tr>
        <tr data-id="2"><td>2</td><td>P-1002</td><td>완제품B</td></tr>
        <tr data-id="3"><td>3</td><td>P-1003</td><td>완제품C</td></tr>
        <tr data-id="4"><td>4</td><td>P-1004</td><td>완제품D</td></tr>
      `;
    });
    await shot(page, '06-lookup-modal.png');

  } finally {
    await browser.close();
  }
}

run().catch((err) => {
  console.error(err);
  process.exit(1);
});
