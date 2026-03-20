const API_BASE = 'http://localhost:8080/api/v1';
const result = document.getElementById('result');
const statusText = document.getElementById('statusText');

const state = {
  workOrders: [],
  productionResults: [],
  inventories: [],
  products: [],
  processes: [],
};

let lookupTarget = null;
let lookupType = null;

const print = (data) => {
  result.textContent = typeof data === 'string' ? data : JSON.stringify(data, null, 2);
};

function setStatus(msg) { statusText.textContent = msg; }

async function request(url, method = 'GET', body) {
  const res = await fetch(url, {
    method,
    headers: { 'Content-Type': 'application/json' },
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
    setStatus('READY');
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
  } catch (err) {
    setStatus('ERROR');
    print(err);
  }

  renderProdResults();
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
});

renderWorkOrders();
renderProdResults();
renderInventory();
loadInitialData();
