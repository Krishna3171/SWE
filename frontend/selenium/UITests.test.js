const { Builder, By, until } = require('selenium-webdriver');
const chrome = require('selenium-webdriver/chrome');
const fs = require('fs');
const path = require('path');

jest.setTimeout(30000); // Increase timeout for Selenium tests

describe('Pharmacy Management System UI Tests', () => {
  let driver;
  const BASE_URL = 'http://localhost:3000';
  let options;

  const resetBrowserState = async () => {
    await driver.get(BASE_URL);
    await driver.manage().deleteAllCookies();
    await driver.executeScript('window.localStorage.clear(); window.sessionStorage.clear();');
    await driver.navigate().refresh();
  };

  const loginAsAdmin = async () => {
    const attemptLogin = async () => {
      const usernameInput = await driver.wait(until.elementLocated(By.id('username')), 10000);
      await usernameInput.clear();
      await usernameInput.sendKeys('admin123');

      const passwordInput = await driver.wait(until.elementLocated(By.id('password')), 5000);
      await passwordInput.clear();
      await passwordInput.sendKeys('admin');

      const signInButton = await driver.findElement(By.css('button[type="submit"]'));
      await signInButton.click();
      return driver.wait(until.elementLocated(By.css('.dashboard-layout')), 15000);
    };

    try {
      await attemptLogin();
    } catch (firstError) {
      await resetBrowserState();
      await attemptLogin();
    }
  };

  beforeAll(async () => {
    // Set up Chrome options
    options = new chrome.Options();
    options.addArguments('--headless'); // Run in headless mode
    options.addArguments('--no-sandbox');
    options.addArguments('--disable-dev-shm-usage');
  });

  afterEach(async () => {
    if (driver) {
      if (expect.getState().currentTestName && expect.getState().lastExpectationStatus === 'failed') {
        const screenshot = await driver.takeScreenshot();
        const screenshotPath = path.join(__dirname, `test-failure-${Date.now()}.png`);
        fs.writeFileSync(screenshotPath, screenshot, 'base64');
        console.log(`Screenshot saved to: ${screenshotPath}`);
      }
      await driver.quit();
      driver = null;
    }
  });

  beforeEach(async () => {
    // Fresh browser per test avoids auth/session leakage.
    driver = await new Builder()
      .forBrowser('chrome')
      .setChromeOptions(options)
      .build();
    await resetBrowserState();
  });

  test('should display login page', async () => {
    const title = await driver.getTitle();
    expect(title).toContain('MSA Portal');

    const heading = await driver.findElement(By.tagName('h1'));
    const headingText = await heading.getText();
    expect(headingText).toBe('MSA Portal');
  });

  test('should login as admin successfully', async () => {
    await loginAsAdmin();
  });

  test('should show error for invalid login', async () => {
    // Enter invalid username
    const usernameInput = await driver.findElement(By.id('username'));
    await usernameInput.clear();
    await usernameInput.sendKeys('invaliduser');

    // Enter invalid password
    const passwordInput = await driver.findElement(By.id('password'));
    await passwordInput.clear();
    await passwordInput.sendKeys('wrongpassword');

    // Click sign in button
    const signInButton = await driver.findElement(By.css('button[type="submit"]'));
    await signInButton.click();

    // Wait for error message
    const errorElement = await driver.wait(until.elementLocated(By.css('.error-text')), 5000);
    const errorText = await errorElement.getText();
    expect(errorText).toContain('Invalid credentials');
  });

  test('should navigate to medicine management', async () => {
    await loginAsAdmin();

    // Click on Medicine Management
    const medicineLink = await driver.wait(until.elementLocated(By.xpath("//button[contains(., 'Medicines')]")), 5000);
    await medicineLink.click();

    // Verify we're on medicine management page
    await driver.wait(until.elementLocated(By.xpath("//h2[text()='Medicines']")), 5000);

    // Check if medicine list is displayed
    const medicineTable = await driver.wait(until.elementLocated(By.css('table')), 5000);
    expect(medicineTable).toBeTruthy();
  });

  test('should add new medicine', async () => {
    await loginAsAdmin();

    // Navigate to medicine management
    const medicineLink = await driver.findElement(By.xpath("//button[contains(., 'Medicines')]"));
    await medicineLink.click();

    await driver.wait(until.elementLocated(By.xpath("//h2[text()='Medicines']")), 5000);

    // Verify Add Medicine button exists and opens the modal
    const addButton = await driver.wait(until.elementLocated(By.css('[data-testid="add-medicine-header-btn"]')), 5000);
    expect(addButton).toBeTruthy();
    await addButton.click();

    // Verify modal opens with the form
    const modal = await driver.wait(until.elementLocated(By.css('.modal-overlay')), 5000);
    expect(modal).toBeTruthy();

    const formHeading = await driver.wait(until.elementLocated(By.xpath("//h3[text()='Add New Medicine']")), 5000);
    expect(formHeading).toBeTruthy();

    // Verify key form fields are present
    const tradeNameInput = await driver.wait(until.elementLocated(By.css('[data-testid="trade-name-input"]')), 5000);
    expect(tradeNameInput).toBeTruthy();

    const submitBtn = await driver.wait(until.elementLocated(By.css('[data-testid="submit-medicine-btn"]')), 5000);
    expect(submitBtn).toBeTruthy();
  });
});

