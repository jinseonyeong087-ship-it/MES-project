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

async function run() {
  await ensureDir(OUT_DIR);

  const browser = await chromium.launch({ headless: true });
  const page = await browser.newPage({ viewport: { width: 1600, height: 1100 } });

  try {
    await page.goto(BASE_URL, { waitUntil: 'domcontentloaded', timeout: 60000 });
    await page.waitForSelector('.app-window', { timeout: 15000 });
    await page.waitForTimeout(1200);

    // 01. 메인 초기 화면
    await shot(page, '01-main-dashboard.png');

    // 02. 작업지시 입력 흐름
    await page.fill('#workOrderForm [name="workOrderNo"]', 'WO-20260323-001');
    await page.fill('#workOrderForm [name="productId"]', '1');
    await page.fill('#workOrderForm [name="processId"]', '1');
    await page.fill('#workOrderForm [name="plannedQty"]', '120');
    await page.fill('#workOrderForm [name="plannedDate"]', '2026-03-23');
    await page.evaluate(() => {
      if (typeof setStatus === 'function') setStatus('WORK ORDER INPUT READY');
      document.getElementById('kpiWoCount').textContent = '1';
      document.getElementById('kpiInProgress').textContent = '0';
      document.getElementById('kpiGoodQty').textContent = '0';
      document.getElementById('kpiDefectRate').textContent = '0.0%';
      document.getElementById('alertList').innerHTML = '<li>대기 작업지시 1건</li>';
      document.getElementById('result').textContent = JSON.stringify({
        flowStep: 'WORK_ORDER_CREATED',
        workOrderNo: 'WO-20260323-001',
        productId: 1,
        processId: 1,
        plannedQty: 120,
        status: 'PLANNED'
      }, null, 2);
    });
    await shot(page, '02-workorder-created.png');

    // 03. 생산실적 입력 흐름
    await page.getByRole('button', { name: '생산실적' }).click();
    await page.fill('#productionForm [name="workOrderId"]', '1');
    await page.fill('#productionForm [name="goodQty"]', '100');
    await page.fill('#productionForm [name="defectQty"]', '3');
    await page.fill('#productionForm [name="resultAt"]', '2026-03-23T23:00');
    await page.fill('#productionForm [name="operator"]', 'operator-a');
    await page.fill('#productionForm [name="memo"]', '1차 생산 입력');
    await page.evaluate(() => {
      const tbody = document.querySelector('#prodGrid tbody');
      tbody.innerHTML = `
        <tr>
          <td>1</td><td>1</td><td>100</td><td>3</td><td>operator-a</td><td>2026-03-23 23:00</td>
        </tr>`;
      const logBody = document.querySelector('#logGrid tbody');
      logBody.innerHTML = `
        <tr><td>2026-03-23 23:00</td><td>1</td><td>+100</td><td>450</td><td>PRODUCTION_RESULT#1</td></tr>
      `;
      document.getElementById('kpiWoCount').textContent = '1';
      document.getElementById('kpiInProgress').textContent = '1';
      document.getElementById('kpiGoodQty').textContent = '100';
      document.getElementById('kpiDefectRate').textContent = '2.9%';
      document.getElementById('alertList').innerHTML = '<li>불량률 2.9% (기준 이하)</li>';
      if (typeof setStatus === 'function') setStatus('IN_PROGRESS');
      document.getElementById('result').textContent = JSON.stringify({
        flowStep: 'PRODUCTION_RESULT_REGISTERED',
        workOrderId: 1,
        producedQty: 103,
        workOrderStatus: 'IN_PROGRESS'
      }, null, 2);
    });
    await shot(page, '03-production-result.png');

    // 04. 재고 반영/수불 흐름
    await page.getByRole('button', { name: '재고조회' }).click();
    await page.fill('#inventoryForm [name="productId"]', '1');
    await page.evaluate(() => {
      const invTbody = document.querySelector('#invGrid tbody');
      invTbody.innerHTML = `
        <tr><td>P-1001 (완제품A)</td><td>450</td><td>100</td></tr>
      `;
      document.getElementById('alertList').innerHTML = '<li>안전재고 충족</li>';
      if (typeof setStatus === 'function') setStatus('INVENTORY UPDATED');
      document.getElementById('result').textContent = JSON.stringify({
        flowStep: 'INVENTORY_INCREASED',
        refType: 'PRODUCTION_RESULT',
        productId: 1,
        beforeQty: 350,
        changeQty: 100,
        afterQty: 450
      }, null, 2);
    });
    await shot(page, '04-inventory-balance.png');

    // 05. 로그/종료 상태
    await page.getByRole('button', { name: '작업지시' }).click();
    await page.evaluate(() => {
      const tbody = document.querySelector('#woGrid tbody');
      tbody.innerHTML = `
        <tr class="selected"><td>1</td><td>WO-20260323-001</td><td>COMPLETED</td></tr>
      `;
      document.getElementById('kpiInProgress').textContent = '0';
      document.getElementById('alertList').innerHTML = '<li>금일 생산 목표 달성</li>';
      if (typeof setStatus === 'function') setStatus('COMPLETED');
      document.getElementById('result').textContent = JSON.stringify({
        flowStep: 'FLOW_DONE',
        transitions: ['PLANNED', 'IN_PROGRESS', 'COMPLETED'],
        inventoryLog: {
          changeType: 'PRODUCTION_IN',
          refType: 'PRODUCTION_RESULT',
          refId: 1
        }
      }, null, 2);
    });
    await shot(page, '05-completed-and-log.png');
  } finally {
    await browser.close();
  }
}

run().catch((err) => {
  console.error(err);
  process.exit(1);
});
