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
    const futureDate = new Date();
    futureDate.setFullYear(futureDate.getFullYear() + 1);
    const expiryDateValue = futureDate.toISOString().slice(0, 10);

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
    await expiryInput.sendKeys(expiryDateValue);

    const reorderInput = await driver.findElement(By.name('reorderThreshold'));
    await reorderInput.sendKeys('5');

    const vendorInput = await driver.findElement(By.name('vendorId'));
    await vendorInput.sendKeys('1');

    // Submit the form
    const submitButton = await driver.findElement(By.css('button[type="submit"]'));
    await submitButton.click();

    // Wait for success OR fail fast if any visible error appears.
    const rowLocator = By.xpath(`//*[contains(text(), "Selenium Test Medicine ${uniqueSuffix}")]`);
    const outcome = await driver.wait(async () => {
      const errorToasts = await driver.findElements(By.css('.toast.error'));
      if (errorToasts.length > 0) {
        const msg = await errorToasts[0].getText();
        return { status: 'error', msg };
      }

      const successToasts = await driver.findElements(By.css('.toast.success'));
      if (successToasts.length > 0) {
        return { status: 'success' };
      }

      const insertedRows = await driver.findElements(rowLocator);
      if (insertedRows.length > 0) {
        return { status: 'success' };
      }

      return false;
    }, 20000);

    if (outcome.status === 'error') {
      throw new Error(`Add medicine failed: ${outcome.msg}`);
    }
  });
});

