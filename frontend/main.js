// 브라우저 전역값(window.__API_BASE__)이 있으면 우선 사용
const API_BASE = window.__API_BASE__ || 'http://localhost:8080/api/v1';
const result = document.getElementById('result');
const statusText = document.getElementById('statusText');

const state = {
  workOrders: [],
  productionResults: [],
  inventories: [],
  products: [],
  processes: [],
  inventoryLogs: [],
};

let lookupTarget = null;
let lookupType = null;

const print = (data) => {
  result.textContent = typeof data === 'string' ? data : JSON.stringify(data, null, 2);
};

function updateFlow(status) {
  const planned = document.getElementById('flow-planned');
  const progress = document.getElementById('flow-progress');
  const completed = document.getElementById('flow-completed');
  if (!planned || !progress || !completed) return;

  [planned, progress, completed].forEach(el => el.classList.remove('active', 'done'));

  if (status === 'PLANNED' || status === 'WORK ORDER SAVED' || status === 'WORK ORDER INPUT READY') {
    planned.classList.add('active');
  } else if (status === 'IN_PROGRESS' || status === 'PRODUCTION SAVED') {
    planned.classList.add('done');
    progress.classList.add('active');
  } else if (status === 'COMPLETED' || status === 'INVENTORY UPDATED' || status === 'INVENTORY LOADED') {
    planned.classList.add('done');
    progress.classList.add('done');
    completed.classList.add('active');
  }
}

function setStatus(msg) {
  statusText.textContent = msg;
  updateFlow(msg);
}

async function request(url, method = 'GET', body) {
  const headers = { 'Content-Type': 'application/json' };

  // 운영/스테이징에서 API Key 방식 사용할 수 있도록 지원
  if (window.__API_KEY__) headers['X-API-KEY'] = window.__API_KEY__;
  if (window.__USER_ID__) headers['X-USER-ID'] = window.__USER_ID__;
  if (window.__USER_ROLE__) headers['X-USER-ROLE'] = window.__USER_ROLE__;

  const res = await fetch(url, {
    method,
    headers,
    body: body ? JSON.stringify(body) : undefined,
  });
  const text = await res.text();
  let payload;
  try { payload = JSON.parse(text); } catch { payload = text; }
  if (!res.ok) throw payload;
  return payload;
}

function renderWorkOrders() {
  const tbody = document.querySelector('#woGrid tbody');
  if (!state.workOrders.length) {
    tbody.innerHTML = '<tr><td colspan="3" class="muted">데이터 없음</td></tr>';
    return;
  }
  tbody.innerHTML = state.workOrders.slice(0, 20).map(w => `
    <tr data-id="${w.id}">
      <td>${w.id ?? '-'}</td>
      <td>${w.workOrderNo ?? '-'}</td>
      <td>${w.status ?? 'PLANNED'}</td>
    </tr>
  `).join('');

  tbody.querySelectorAll('tr').forEach(tr => {
    tr.addEventListener('click', () => {
      tbody.querySelectorAll('tr').forEach(r => r.classList.remove('selected'));
      tr.classList.add('selected');

      const id = Number(tr.dataset.id);
      const item = state.workOrders.find(w => w.id === id);
      if (!item) return;

      document.querySelector('#productionForm [name="workOrderId"]').value = item.id;
      document.querySelector('#workOrderForm [name="workOrderNo"]').value = item.workOrderNo || '';
      document.querySelector('#workOrderForm [name="productId"]').value = item.productId || '';
      document.querySelector('#workOrderForm [name="processId"]').value = item.processId || '';
      document.querySelector('#workOrderForm [name="plannedQty"]').value = item.plannedQty || '';
      if (item.plannedDate) document.querySelector('#workOrderForm [name="plannedDate"]').value = item.plannedDate;

      setStatus(`WO SELECTED: ${item.workOrderNo}`);
      updateFlow(item.status || 'PLANNED');
    });
  });
}

function renderProdResults() {
  const tbody = document.querySelector('#prodGrid tbody');
  if (!state.productionResults.length) {
    tbody.innerHTML = '<tr><td colspan="6" class="muted">데이터 없음</td></tr>';
    return;
  }
  tbody.innerHTML = state.productionResults.slice(0, 30).map((p, idx) => `
    <tr>
      <td>${idx + 1}</td>
      <td>${p.workOrderId}</td>
      <td>${p.goodQty}</td>
      <td>${p.defectQty}</td>
      <td>${p.operator}</td>
      <td>${(p.resultAt || '').replace('T', ' ').slice(0, 16)}</td>
    </tr>
  `).join('');
}

function renderInventory() {
  const tbody = document.querySelector('#invGrid tbody');
  if (!state.inventories.length) {
    tbody.innerHTML = '<tr><td colspan="3" class="muted">조회 결과 없음</td></tr>';
    return;
  }
  tbody.innerHTML = state.inventories.map(i => `
    <tr>
      <td>${i.productName || i.productCode || i.productId}</td>
      <td>${i.qtyOnHand}</td>
      <td>${i.safetyStock}</td>
    </tr>
  `).join('');
}

function renderKpi() {
  const woCount = state.workOrders.length;
  const inProgress = state.workOrders.filter(w => w.status === 'IN_PROGRESS').length;
  const good = state.productionResults.reduce((sum, p) => sum + Number(p.goodQty || 0), 0);
  const defect = state.productionResults.reduce((sum, p) => sum + Number(p.defectQty || 0), 0);
  const rate = good + defect ? ((defect / (good + defect)) * 100).toFixed(1) : '0.0';

  document.getElementById('kpiWoCount').textContent = String(woCount);
  document.getElementById('kpiInProgress').textContent = String(inProgress);
  document.getElementById('kpiGoodQty').textContent = String(good);
  document.getElementById('kpiDefectRate').textContent = `${rate}%`;
}

function renderInventoryLogs() {
  const tbody = document.querySelector('#logGrid tbody');
  if (!state.inventoryLogs.length) {
    tbody.innerHTML = '<tr><td colspan="5" class="muted">로그 없음</td></tr>';
    return;
  }

  tbody.innerHTML = state.inventoryLogs.slice(0, 12).map(l => `
    <tr>
      <td>${(l.changedAt || '').replace('T', ' ').slice(0, 16)}</td>
      <td>${l.productId ?? '-'}</td>
      <td>${l.changeQty >= 0 ? `+${l.changeQty}` : l.changeQty}</td>
      <td>${l.afterQty ?? '-'}</td>
      <td>${l.refType || 'PRODUCTION_RESULT'}#${l.refId ?? '-'}</td>
    </tr>
  `).join('');
}

function renderAlerts() {
  const ul = document.getElementById('alertList');
  if (!ul) return;

  const alerts = [];
  const lowStocks = state.inventories.filter(i => Number(i.qtyOnHand) <= Number(i.safetyStock || 0));
  if (lowStocks.length) alerts.push(`안전재고 미만 제품 ${lowStocks.length}건`);

  const defect = state.productionResults.reduce((sum, p) => sum + Number(p.defectQty || 0), 0);
  const total = state.productionResults.reduce((sum, p) => sum + Number(p.defectQty || 0) + Number(p.goodQty || 0), 0);
  if (total > 0 && defect / total >= 0.05) alerts.push(`불량률 ${(defect / total * 100).toFixed(1)}% (기준 5%↑)`);

  const planned = state.workOrders.filter(w => w.status === 'PLANNED').length;
  if (planned > 3) alerts.push(`대기 작업지시 ${planned}건`);

  ul.innerHTML = (alerts.length ? alerts : ['현재 경고 없음']).map(a => `<li>${a}</li>`).join('');
}

for (const btn of document.querySelectorAll('.tb-btn[data-view]')) {
  btn.addEventListener('click', () => {
    document.querySelectorAll('.tb-btn[data-view]').forEach(b => b.classList.remove('active'));
    btn.classList.add('active');
    document.querySelectorAll('.view').forEach(v => v.classList.remove('active'));
    document.getElementById(`view-${btn.dataset.view}`).classList.add('active');
  });
}

document.getElementById('btnHealth').addEventListener('click', async () => {
  try {
    const data = await request(`${API_BASE}/health`);
    setStatus('ONLINE');
    print(data);
  } catch (err) {
    setStatus('OFFLINE');
    print(err);
  }
});

// Enter 키로 다음 필드 이동
function bindEnterNav(form) {
  const fields = Array.from(form.querySelectorAll('input, button[type="submit"]'));
  fields.forEach((el, idx) => {
    if (el.type === 'button') return;
    el.addEventListener('keydown', (e) => {
      if (e.key !== 'Enter') return;
      e.preventDefault();
      const next = fields[idx + 1];
      if (next) next.focus();
      else form.requestSubmit();
    });
  });
}
document.querySelectorAll('.enter-nav').forEach(bindEnterNav);

// Lookup modal
const lookupModal = document.getElementById('lookupModal');
const lookupSearch = document.getElementById('lookupSearch');
const lookupBody = document.querySelector('#lookupGrid tbody');
const lookupTitle = document.getElementById('lookupTitle');

function openLookup(type, targetInput) {
  lookupType = type;
  lookupTarget = targetInput;
  lookupModal.classList.remove('hidden');
  lookupSearch.value = '';
  lookupTitle.textContent = type.startsWith('process') ? '공정 코드 도움' : '제품 코드 도움';
  renderLookupRows();
  lookupSearch.focus();
}
function closeLookup() {
  lookupModal.classList.add('hidden');
  lookupType = null;
  lookupTarget = null;
}

function getLookupRows() {
  const raw = lookupType?.startsWith('process') ? state.processes : state.products;
  const q = lookupSearch.value.trim().toLowerCase();
  if (!q) return raw;
  return raw.filter(r => String(r.id).includes(q) || (r.code || '').toLowerCase().includes(q) || (r.name || '').toLowerCase().includes(q));
}

function renderLookupRows() {
  const rows = getLookupRows();
  lookupBody.innerHTML = rows.map(r => `
    <tr data-id="${r.id}">
      <td>${r.id}</td>
      <td>${r.code}</td>
      <td>${r.name}</td>
    </tr>
  `).join('') || '<tr><td colspan="3" class="muted">검색 결과 없음</td></tr>';

  lookupBody.querySelectorAll('tr[data-id]').forEach(tr => {
    tr.addEventListener('dblclick', () => {
      if (!lookupTarget) return;
      lookupTarget.value = tr.dataset.id;
      closeLookup();
      lookupTarget.focus();
    });
  });
}

lookupSearch.addEventListener('input', renderLookupRows);
document.getElementById('closeLookup').addEventListener('click', closeLookup);
lookupModal.addEventListener('click', (e) => { if (e.target === lookupModal) closeLookup(); });

document.querySelectorAll('.lookup-btn').forEach(btn => {
  btn.addEventListener('click', () => {
    const type = btn.dataset.lookup;
    const input = btn.closest('.inline-field').querySelector('input');
    openLookup(type, input);
  });
});

async function loadInitialData() {
  setStatus('LOADING...');
  try {
    const [workOrders, productionResults, products, processes] = await Promise.all([
      request(`${API_BASE}/work-orders`),
      request(`${API_BASE}/production-results`),
      request(`${API_BASE}/products`),
      request(`${API_BASE}/processes`),
    ]);

    state.workOrders = Array.isArray(workOrders) ? workOrders : [];
    state.productionResults = Array.isArray(productionResults)
      ? productionResults.map(p => ({
          workOrderId: p.workOrderId,
          goodQty: p.goodQty,
          defectQty: p.defectQty,
          operator: p.operator,
          resultAt: p.resultAt,
        }))
      : [];
    state.products = Array.isArray(products) ? products : [];
    state.processes = Array.isArray(processes) ? processes : [];

    renderWorkOrders();
    renderProdResults();
    renderInventory();
    renderInventoryLogs();
    renderKpi();
    renderAlerts();
    setStatus('READY');
    if (state.workOrders.length) updateFlow(state.workOrders[0].status || 'PLANNED');
  } catch (err) {
    setStatus('INIT LOAD ERROR');
    print(err);
  }
}

// submit handlers
document.getElementById('workOrderForm').addEventListener('submit', async (e) => {
  e.preventDefault();
  const f = new FormData(e.target);
  const body = {
    workOrderNo: f.get('workOrderNo'),
    productId: Number(f.get('productId')),
    processId: Number(f.get('processId')),
    plannedQty: Number(f.get('plannedQty')),
    plannedDate: f.get('plannedDate')
  };

  try {
    const data = await request(`${API_BASE}/work-orders`, 'POST', body);
    state.workOrders.unshift(data);
    setStatus('WORK ORDER SAVED');
    print(data);
  } catch (err) {
    setStatus('ERROR');
    print(err);
  }
  renderWorkOrders();
  renderKpi();
  renderAlerts();
});

document.getElementById('productionForm').addEventListener('submit', async (e) => {
  e.preventDefault();
  const f = new FormData(e.target);
  const body = {
    workOrderId: Number(f.get('workOrderId')),
    goodQty: Number(f.get('goodQty')),
    defectQty: Number(f.get('defectQty')),
    resultAt: new Date(f.get('resultAt')).toISOString(),
    operator: f.get('operator'),
    memo: f.get('memo') || null
  };

  try {
    const data = await request(`${API_BASE}/production-results`, 'POST', body);
    setStatus('PRODUCTION SAVED');
    print(data);
    state.productionResults.unshift(body);

    const beforeQty = Number(data?.inventorySnapshot?.beforeQty ?? data?.beforeQty ?? 0);
    const afterQty = Number(data?.inventorySnapshot?.afterQty ?? data?.afterQty ?? beforeQty + Number(body.goodQty || 0));
    const productId = data?.productId ?? data?.inventorySnapshot?.productId ?? null;

    state.inventoryLogs.unshift({
      changedAt: new Date().toISOString(),
      productId,
      changeQty: Number(body.goodQty || 0),
      afterQty,
      refType: 'PRODUCTION_RESULT',
      refId: data?.productionResultId ?? data?.id ?? null,
    });
  } catch (err) {
    setStatus('ERROR');
    print(err);
  }

  renderProdResults();
  renderInventoryLogs();
  renderKpi();
  renderAlerts();
});

document.getElementById('inventoryForm').addEventListener('submit', async (e) => {
  e.preventDefault();
  const f = new FormData(e.target);
  const productId = Number(f.get('productId'));

  try {
    const data = await request(`${API_BASE}/inventories/${productId}`);
    const idx = state.inventories.findIndex(i => i.productId === data.productId);
    if (idx >= 0) state.inventories[idx] = data;
    else state.inventories.unshift(data);
    setStatus('INVENTORY LOADED');
    print(data);
  } catch (err) {
    setStatus('ERROR');
    print(err);
  }
  renderInventory();
  renderKpi();
  renderAlerts();
});

renderWorkOrders();
renderProdResults();
renderInventory();
renderInventoryLogs();
renderKpi();
renderAlerts();
loadInitialData();
