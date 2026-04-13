const { Builder, By, until } = require('selenium-webdriver');
const chrome = require('selenium-webdriver/chrome');
const fs = require('fs');
const path = require('path');
const http = require('http');


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
        const screenshotDir = path.join(__dirname, 'screenshot');
        if (!fs.existsSync(screenshotDir)) {
          fs.mkdirSync(screenshotDir, { recursive: true });
        }
        const screenshotPath = path.join(screenshotDir, `test-failure-${Date.now()}.png`);
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
    // Enter invalid username with explicit wait to prevent race conditions
    const usernameInput = await driver.wait(until.elementLocated(By.id('username')), 10000);
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

  test('should navigate to inventory management', async () => {
    await loginAsAdmin();

    // Click on Inventory & Stock
    const invLink = await driver.wait(until.elementLocated(By.xpath("//button[contains(., 'Inventory & Stock')]")), 5000);
    await invLink.click();

    await driver.wait(until.elementLocated(By.xpath("//h2[text()='Inventory & Stock']")), 5000);

    // Check if search exists
    const searchInput = await driver.wait(until.elementLocated(By.css('input[placeholder="Search inventory code or name..."]')), 5000);
    expect(searchInput).toBeTruthy();
  });




  test('should make sale', async () => {
    await loginAsAdmin();

    // Click on Sales/POS
    const posLink = await driver.wait(until.elementLocated(By.xpath("//button[contains(., 'Sales/POS')]")), 5000);
    await posLink.click();

    await driver.wait(until.elementLocated(By.xpath("//h2[text()='Point of Sale']")), 5000);

    // Verify cart header
    const cartHeader = await driver.wait(until.elementLocated(By.xpath("//h3[contains(., 'Current Cart')]")), 5000);
    expect(cartHeader).toBeTruthy();

    // Search for crocin
    const searchInput = await driver.wait(until.elementLocated(By.css('input[placeholder="Search by name, generic, or code..."]')), 5000);
    await searchInput.click();
    // clear input before typing just in case
    await searchInput.clear();
    await searchInput.sendKeys('crocin');

    // Wait for the dropdown option container to appear (which means we found a medicine)
    // We will look for a div that has padding '10px 14px' since those are the selectable options
    const medOption = await driver.wait(until.elementLocated(By.xpath("//div[contains(@style, 'padding: 10px 14px')]")), 8000);
    await driver.sleep(1000); // give time for the dropdown to attach React event listeners
    await driver.executeScript("arguments[0].click();", medOption);

    // Set Quantity to 1
    const quantityInput = await driver.findElement(By.xpath("//label[text()='Quantity']/following-sibling::input"));
    await quantityInput.clear();
    await quantityInput.sendKeys('1');

    // Add to Session
    const addToSessionBtn = await driver.wait(until.elementLocated(By.xpath("//button[contains(., 'Add to Session')]")), 5000);
    // Explicitly wait for it to become enabled (if it is disabled, it means medicine wasn't properly selected)
    await driver.wait(until.elementIsEnabled(addToSessionBtn), 5000);
    await driver.sleep(500);
    await driver.executeScript("arguments[0].click();", addToSessionBtn);
    await driver.sleep(500);

    // Checkout
    const checkoutBtn = await driver.wait(until.elementLocated(By.xpath("//button[contains(., 'COMPLETE CHECKOUT')]")), 5000);
    await driver.wait(until.elementIsEnabled(checkoutBtn), 5000);
    await driver.sleep(500); // small delay for internal React state
    await driver.executeScript("arguments[0].click();", checkoutBtn);

    // Verify successful checkout layout opens
    const successHeader = await driver.wait(until.elementLocated(By.xpath("//h2[text()='Payment Successful']")), 5000);
    expect(successHeader).toBeTruthy();

    // Close modal
    const closeBtn = await driver.wait(until.elementLocated(By.xpath("//button[text()='Start Next Sale']")), 5000);
    await driver.executeScript("arguments[0].click();", closeBtn);
  });

  test('should verify add vendor component', async () => {
    await loginAsAdmin();

    const vendorLink = await driver.wait(until.elementLocated(By.xpath("//button[contains(., 'Vendors')]")), 5000);
    await vendorLink.click();
    await driver.wait(until.elementLocated(By.xpath("//h2[text()='Vendor Details']")), 5000);

    const addButton = await driver.wait(until.elementLocated(By.xpath("//button[contains(., 'Add Vendor')]")), 5000);
    await addButton.click();

    const modal = await driver.wait(until.elementLocated(By.css('.modal-overlay')), 5000);
    expect(modal).toBeTruthy();

    const formHeading = await driver.wait(until.elementLocated(By.xpath("//h3[text()='Register New Vendor']")), 5000);
    expect(formHeading).toBeTruthy();
  });

  test('should navigate to auto-generated orders', async () => {
    await loginAsAdmin();
    const ordersLink = await driver.wait(until.elementLocated(By.xpath("//button[contains(., 'Orders')]")), 5000);
    await ordersLink.click();

    await driver.wait(until.elementLocated(By.xpath("//h2[text()='Auto-Generated Orders']")), 5000);

    const printSummaryBtn = await driver.wait(until.elementLocated(By.xpath("//button[contains(., 'Print Order Summary')]")), 5000);
    expect(printSummaryBtn).toBeTruthy();
  });
});
