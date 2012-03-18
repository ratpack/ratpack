public abstract class DirWatcher extends TimerTask {
  def path
  def dir = [:]
  // Exclude temp files created by vim, emacs, etc...
  FileFilter fileFilter = {file -> !(file.name =~ /\.swp$|\~$|^\./)} as FileFilter
  
  public DirWatcher(String path) {
    this.path = path;
    def files = new File(path).listFiles(fileFilter);
    
    // transfer to the hashmap be used a reference and keep the lastModfied value
    for(File file : files) {
      dir.put(file, file.lastModified());
    }
  }
  
  public final void run() {
    def checkedFiles = new HashSet();
    def files = new File(path).listFiles(fileFilter);
    
    // scan the files and check for modification/addition
    for (File file : files) { 
      Long current = dir.get(file)
      checkedFiles.add(file)
      if (current == null) {
        // new file
        dir.put(file, new Long(file.lastModified()))
        onChange(file, "add")
      }
      else if (current.longValue() != file.lastModified()){
        // modified file
        dir.put(file, new Long(file.lastModified()))
        onChange(file, "modify")
      }
    }
    
    // now check for deleted files
    def deletedFiles = dir.clone().keySet() - checkedFiles
    deletedFiles.each {
      dir.remove(it)
      onChange(it, "delete") 
    }
  }
  
  protected abstract void onChange(File file, String action);
}

class AppRunner extends DirWatcher {
  def proc = null
  def script
  AppRunner(String script, String path) {
    super(path)
    this.script = script
  }

  def manageApp() {
    runApp()
    Timer timer = new Timer()
    timer.schedule(this, new Date(), 1000)
  }

  def runApp() {
    proc = "${path}/ratpack ${script}".execute()
    proc.consumeProcessOutput(System.out, System.err)
  }

  def killApp() {
    proc.waitForOrKill(1000) 
  }

  void onChange(File file, String action) {
    println ("File "+ file.name +" action: " + action )
    if (proc) {
      println "KILLING"
      killApp()
      println "RELOADING"
    } else {
      println "STARTING" 
    }
    runApp()
  }
}

if (args.length == 2) {
  new AppRunner(args[0], args[1]).manageApp()
} else {
  println "Usage:"
  println "groovy runapp.groovy [script] [dir to watch]"
}
