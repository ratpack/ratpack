import geb.buildadapter.BuildAdapterFactory
import org.openqa.selenium.Dimension
import org.openqa.selenium.Point
import org.openqa.selenium.chrome.ChromeDriver
import ratpack.site.TestConfig

import java.awt.*

if (!BuildAdapterFactory.getBuildAdapter(this.class.classLoader).reportsDir) {
  reportsDir = "build/geb"
}

driver = {
  def chromeDriverPath = new TestConfig().chromeDriverPath
  if (chromeDriverPath) {
    System.setProperty("webdriver.chrome.driver", chromeDriverPath)
  }

  def driverInstance = new ChromeDriver()
  def screenSize = Toolkit.defaultToolkit.screenSize
  def browserWindow = driverInstance.manage().window()
  browserWindow.size = new Dimension(screenSize.width.toInteger(), screenSize.height.toInteger())
  browserWindow.position = new Point(0, 0)
  driverInstance
}