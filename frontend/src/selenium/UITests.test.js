const { Builder, By, until } = require('selenium-webdriver');
const chrome = require('selenium-webdriver/chrome');

describe('Pharmacy Management System UI Tests', () => {
  let driver;

  beforeAll(async () => {
    // Set up Chrome options
    const options = new chrome.Options();
    options.addArguments('--headless'); // Run in headless mode
    options.addArguments('--no-sandbox');
    options.addArguments('--disable-dev-shm-usage');

    // Build the WebDriver instance
    driver = await new Builder()
      .forBrowser('chrome')
      .setChromeOptions(options)
      .build();
  });

  afterAll(async () => {
    if (driver) {
      await driver.quit();
    }
  });

  beforeEach(async () => {
    await driver.get('http://localhost:3000'); // React app URL
  });

  test('should display login page', async () => {
    const title = await driver.getTitle();
    expect(title).toContain('MSA Portal');

    const heading = await driver.findElement(By.tagName('h1'));
    const headingText = await heading.getText();
    expect(headingText).toBe('MSA Portal');
  });

  test('should login as admin successfully', async () => {
    // Select role
    const roleSelect = await driver.findElement(By.id('role'));
    await roleSelect.sendKeys('admin');

    // Enter username
    const usernameInput = await driver.findElement(By.id('username'));
    await usernameInput.sendKeys('admin');

    // Enter password
    const passwordInput = await driver.findElement(By.id('password'));
    await passwordInput.sendKeys('admin123');

    // Click sign in button
    const signInButton = await driver.findElement(By.css('button[type="submit"]'));
    await signInButton.click();

    // Wait for navigation or success message
    await driver.wait(until.urlContains('/dashboard'), 10000);

    const currentUrl = await driver.getCurrentUrl();
    expect(currentUrl).toContain('/dashboard');
  });

  test('should show error for invalid login', async () => {
    // Select role
    const roleSelect = await driver.findElement(By.id('role'));
    await roleSelect.sendKeys('admin');

    // Enter invalid username
    const usernameInput = await driver.findElement(By.id('username'));
    await usernameInput.sendKeys('invaliduser');

    // Enter invalid password
    const passwordInput = await driver.findElement(By.id('password'));
    await passwordInput.sendKeys('wrongpassword');

    // Click sign in button
    const signInButton = await driver.findElement(By.css('button[type="submit"]'));
    await signInButton.click();

    // Wait for error message
    const errorElement = await driver.wait(until.elementLocated(By.css('.error-message')), 5000);
    const errorText = await errorElement.getText();
    expect(errorText).toContain('Invalid credentials');
  });

  test('should navigate to medicine management', async () => {
    // First login as admin
    const roleSelect = await driver.findElement(By.id('role'));
    await roleSelect.sendKeys('admin');

    const usernameInput = await driver.findElement(By.id('username'));
    await usernameInput.sendKeys('admin');

    const passwordInput = await driver.findElement(By.id('password'));
    await passwordInput.sendKeys('admin123');

    const signInButton = await driver.findElement(By.css('button[type="submit"]'));
    await signInButton.click();

    await driver.wait(until.urlContains('/dashboard'), 10000);

    // Click on Medicine Management
    const medicineLink = await driver.wait(until.elementLocated(By.linkText('Medicine Management')), 5000);
    await medicineLink.click();

    // Verify we're on medicine management page
    await driver.wait(until.urlContains('/medicines'), 5000);
    const currentUrl = await driver.getCurrentUrl();
    expect(currentUrl).toContain('/medicines');

    // Check if medicine list is displayed
    const medicineTable = await driver.wait(until.elementLocated(By.css('table')), 5000);
    expect(medicineTable).toBeTruthy();
  });

  test('should add new medicine', async () => {
    // Login first
    const roleSelect = await driver.findElement(By.id('role'));
    await roleSelect.sendKeys('admin');

    const usernameInput = await driver.findElement(By.id('username'));
    await usernameInput.sendKeys('admin');

    const passwordInput = await driver.findElement(By.id('password'));
    await passwordInput.sendKeys('admin123');

    const signInButton = await driver.findElement(By.css('button[type="submit"]'));
    await signInButton.click();

    await driver.wait(until.urlContains('/dashboard'), 10000);

    // Navigate to medicine management
    const medicineLink = await driver.findElement(By.linkText('Medicine Management'));
    await medicineLink.click();

    await driver.wait(until.urlContains('/medicines'), 5000);

    // Click Add Medicine button
    const addButton = await driver.findElement(By.text('Add Medicine'));
    await addButton.click();

    // Fill the form
    const tradeNameInput = await driver.findElement(By.name('tradeName'));
    await tradeNameInput.sendKeys('Selenium Test Medicine');

    const genericNameInput = await driver.findElement(By.name('genericName'));
    await genericNameInput.sendKeys('Test Generic');

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

    // Wait for success message
    const successMessage = await driver.wait(until.elementLocated(By.text('Medicine Added Successfully!')), 5000);
    expect(successMessage).toBeTruthy();
  });
});