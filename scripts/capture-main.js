const { chromium } = require('playwright');

(async () => {
  const url = process.argv[2] || 'http://host.docker.internal:5500';
  const out = process.argv[3] || '/out/main-screen.png';

  const browser = await chromium.launch({ headless: true });
  const page = await browser.newPage({ viewport: { width: 1600, height: 1000 } });

  try {
    await page.goto(url, { waitUntil: 'domcontentloaded', timeout: 60000 });
    await page.waitForSelector('.app-window', { timeout: 15000 });

    // 한글 폰트 강제 적용 (컨테이너/헤드리스 환경 글꼴 누락 대응)
    await page.addStyleTag({
      content: `
        @import url('https://fonts.googleapis.com/css2?family=Noto+Sans+KR:wght@400;500;700&display=swap');
        html, body, input, button, select, textarea, table, th, td {
          font-family: 'Noto Sans KR', sans-serif !important;
        }
      `,
    });

    // 웹폰트 로딩 완료 대기
    await page.evaluate(async () => {
      if (document.fonts && document.fonts.ready) {
        await document.fonts.ready;
      }
    });

    await page.waitForTimeout(1200);

    await page.screenshot({ path: out, fullPage: true });
    console.log(`saved: ${out}`);
  } catch (err) {
    console.error('capture failed:', err);
    process.exitCode = 1;
  } finally {
    await browser.close();
  }
})();
