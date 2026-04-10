const { Builder, By, until } = require('selenium-webdriver');
const chrome = require('selenium-webdriver/chrome');

(async function() {
  const options = new chrome.Options();
  options.addArguments('--headless', '--no-sandbox', '--disable-dev-shm-usage');
  let driver = await new Builder().forBrowser('chrome').setChromeOptions(options).build();
  
  try {
    await driver.get('http://localhost:3000');
    console.log("Got page");
    
    // Login 1
    await driver.findElement(By.id('username')).sendKeys('admin123');
    await driver.findElement(By.id('password')).sendKeys('admin');
    await driver.findElement(By.css('button[type="submit"]')).click();
    await driver.wait(until.elementLocated(By.css('.dashboard-layout')), 5000);
    console.log("Login 1 success");

    // "Invalid"
    await driver.get('http://localhost:3000');
    await driver.findElement(By.id('username')).sendKeys('invaliduser');
    await driver.findElement(By.id('password')).sendKeys('wrongpassword');
    await driver.findElement(By.css('button[type="submit"]')).click();
    await driver.wait(until.elementLocated(By.css('.error-text')), 5000);
    console.log("Invalid login success");

    // Login 2 (test #4)
    await driver.get('http://localhost:3000');
    await driver.findElement(By.id('username')).sendKeys('admin123');
    await driver.findElement(By.id('password')).sendKeys('admin');
    await driver.findElement(By.css('button[type="submit"]')).click();
    
    try {
        await driver.wait(until.elementLocated(By.css('.dashboard-layout')), 5000);
        console.log("Login 2 success");
    } catch(err) {
        let errText = await driver.findElement(By.css('.error-text')).getText().catch(()=>"No error text shown");
        console.log("Login 2 failed! Error on screen:", errText);
    }
    
  } catch (err) {
      console.error(err);
  } finally {
    await driver.quit();
  }
})();
