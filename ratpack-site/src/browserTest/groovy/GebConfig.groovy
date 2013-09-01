import geb.buildadapter.BuildAdapterFactory
import geb.driver.SauceLabsDriverFactory
import org.openqa.selenium.Dimension
import org.openqa.selenium.Point
import org.openqa.selenium.chrome.ChromeDriver

import java.awt.*

if (!BuildAdapterFactory.getBuildAdapter(this.class.classLoader).reportsDir) {
  reportsDir = "build/geb"
}

driver = {
  def sauceBrowser = System.getProperty("geb.sauce.browser")
  if (sauceBrowser) {
    def username = System.getenv("GEB_SAUCE_LABS_USER")
    assert username
    def accessKey = System.getenv("GEB_SAUCE_LABS_ACCESS_PASSWORD")
    assert accessKey
    new SauceLabsDriverFactory().create(sauceBrowser, username, accessKey)
  } else {
    def driverLocation = System.getProperty('webdriver.chrome.driver') ?: '.gradle/webdriver/driver/chromedriver'
    System.setProperty('webdriver.chrome.driver', driverLocation)
    def driverInstance = new ChromeDriver()
    def screenSize = Toolkit.defaultToolkit.screenSize
    def browserWindow = driverInstance.manage().window()
    browserWindow.size = new Dimension(screenSize.width.toInteger(), screenSize.height.toInteger())
    browserWindow.position = new Point(0, 0)
    driverInstance
  }
}