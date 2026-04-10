const { Builder, By, until } = require('selenium-webdriver');
const chrome = require('selenium-webdriver/chrome');

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
      const usernameInput = await driver.findElement(By.id('username'));
      await usernameInput.clear();
      await usernameInput.sendKeys('admin123');

      const passwordInput = await driver.findElement(By.id('password'));
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

    // Click Add Medicine button
    const addButton = await driver.findElement(By.xpath("//button[contains(., 'Add Medicine')]"));
    await addButton.click();

    const uniqueSuffix = Date.now().toString().slice(-6);

    // Fill the form
    const tradeNameInput = await driver.findElement(By.name('tradeName'));
    await tradeNameInput.sendKeys(`Selenium Test Medicine ${uniqueSuffix}`);

    const genericNameInput = await driver.findElement(By.name('genericName'));
    await genericNameInput.sendKeys(`Test Generic ${uniqueSuffix}`);

    const sellingPriceInput = await driver.findElement(By.name('unitSellingPrice'));
    await sellingPriceInput.sendKeys('20.00');

    const purchasePriceInput = await driver.findElement(By.name('unitPurchasePrice'));
    await purchasePriceInput.sendKeys('15.00');

    const quantityInput = await driver.findElement(By.name('initialQuantity'));
    await quantityInput.sendKeys('50');

    const expiryInput = await driver.findElement(By.name('expiryDate'));
    await expiryInput.sendKeys('2025-12-31');

    const reorderInput = await driver.findElement(By.name('reorderThreshold'));
    await reorderInput.sendKeys('5');

    const vendorInput = await driver.findElement(By.name('vendorId'));
    await vendorInput.sendKeys('1');

    // Submit the form
    const submitButton = await driver.findElement(By.css('button[type="submit"]'));
    await submitButton.click();

    // Wait for a reliable success signal (toast or newly inserted row text).
    const rowLocator = By.xpath(`//*[contains(text(), "Selenium Test Medicine ${uniqueSuffix}")]`);
    try {
      await driver.wait(
        async () => {
          const successToasts = await driver.findElements(By.css('.toast.success'));
          if (successToasts.length > 0) return true;
          const insertedRows = await driver.findElements(rowLocator);
          return insertedRows.length > 0;
        },
        15000,
      );
    } catch (e) {
      const errorToasts = await driver.findElements(By.css('.toast.error'));
      if (errorToasts.length > 0) {
        const errorText = await errorToasts[0].getText();
        throw new Error(`Add medicine failed: ${errorText}`);
      }
      throw e;
    }
  });
});

