package net.krsmes.bukkit.groovy

import org.bukkit.event.Event
import org.bukkit.entity.Player
import java.util.zip.ZipFile


class GroovyRunner extends GroovyAPI {

	static SCRIPT_PREFIX = """
import org.bukkit.*;import org.bukkit.block.*;import org.bukkit.entity.*;import org.bukkit.inventory.*;import org.bukkit.material.*;import org.bukkit.potion.*;import org.bukkit.util.*;
"""

    GroovyPlugin plugin
    GroovyShell shell
    Map vars

    Map<String,Object> global
	Map<String,Object> data = [:]
    Player player = null
    List<String> listeners = []


	GroovyRunner(def plugin, def data) {
		super(plugin.server)
		this.plugin = plugin
        this.global = plugin.runner?.data ?: data
		this.data = data
		_initShell(data)
	}


	def _init() {
		def dir = startupFolder
		if (dir.exists()) {
            // run scripts
            dir.eachFileMatch(groovy.io.FileType.FILES, ~/.*\.groovy/) { f ->
                log "Initializing ${f.name}"
                def result = run(f.text)
                if (result instanceof Map) {
                    listen(f.name, result)
                }
            }
        }
		this
	}

    void _shutdown() {
        listeners.clone().each { unlisten(it) }
    }

    def getStartupFolder() {
        def result = new File(plugin.dataFolder, GroovyPlugin.STARTUP_LOC)
        result.mkdirs()
        result
    }


	def runScript(def script) {
		run SCRIPT_PREFIX + script, null
	}


    def load(name) {
        def file = null
        if (name instanceof File) {
            file = name
        }
        else if (!data.scriptLoc) {
            file = new File(plugin.dataFolder, GroovyPlugin.SCRIPT_LOC + name)
        }
        else {
            def fullName = data.scriptLoc + name
            try {
                URL u = fullName.toURL()
                return u.text
            }
            catch (e) {
                if (!fullName.startsWith('http:')) {
                    file = new File(fullName)
                }
            }
        }
        if (file?.exists()) {
            return file.text
        }
    }


    def runFile = { scriptName, args ->
		def script = load(scriptName + GroovyPlugin.SCRIPT_SUFFIX)
		if (script) {
			run script, args
		}
	}


	def run = { script, args=[] ->
		def result = null
		if (script) {
			try {
				_updateVars()
				def savedArgs = vars.args
				vars.args = args

				result = _parse(script).run()

				vars.args = savedArgs
			}
			catch (e) {
				result = e.message
				e.printStackTrace()
			}
		}
		result
	}

	def _parse = { script ->
		def gscript = shell.parse(script)
        _log.fine("parse: $script")

		gscript.metaClass.methodMissing = { mname, margs ->
            _log.fine("missingMethod $mname: $margs")
            // try method on this class
			if (this.respondsTo(mname, margs)) {
                _log.fine("missingMethod: invoke")
                try {
				    return this.invokeMethod(mname, margs)
                }
                catch (groovy.lang.MissingMethodException e) {}
			}
            // try plugin command
            if (isCommand(mname)) {
                _log.fine("missingMethod: command")
                return runCommand(mname, margs)
            }
            else {
                // try script file
                _log.fine("missingMethod: script")
                return runFile(mname, margs)
            }
		}

		gscript.metaClass.propertyMissing = { pname ->
			if (data.containsKey(pname)) return data[pname]
			def globalData = plugin.runner?.data
			if (globalData?.containsKey(pname)) return globalData[pname]
			server.onlinePlayers.find { it.name.startsWith(pname) }
		}

		gscript
	}


	def _initShell(data) {
		shell = new GroovyShell(plugin.class.classLoader)
		vars = shell.context.variables
		vars.g = this
		vars.s = server
		vars.global = global
		vars.data = data
        vars.Util = net.krsmes.bukkit.groovy.Util
        shell
	}


	void _updateVars() {
		vars.w = world
		vars.spawn = world.spawnLocation

		def pl = [:]
		plugin.server.onlinePlayers.each { pl[it.name] = it }
		vars.pl = pl
	}



//
// listener registration
//

	void listen(String name, def type, Closure c) {
        debug "listen(name=$name, type=$type, c=$c)"
		listen(name, [(type):c])
	}

    void listen(String name, Map typedClosureMap) {
        debug "listen(name=$name, ...)"
        def uniqueName = (player?.name?:'server') + ':' + name
        ListenerClosures.instance.unregister(uniqueName);

        typedClosureMap.each { def type, closure ->
            def eventType
            try {
                eventType = (type instanceof Class) ? type : Class.forName("org.bukkit.event.${eventType}Event")
            }
            catch (e) {
                eventType = null
            }
            if (closure instanceof Closure && eventType) {
                ListenerClosures.instance.register(uniqueName, eventType, closure)
            }
        }
        listeners << uniqueName
    }


    void unlisten(String uniqueName) {
        ListenerClosures.instance.unregister(uniqueName);
        listeners.remove(uniqueName)
    }


    void execute(Closure closure, Event e) {
        if (closure.maximumNumberOfParameters == 1) {
            closure(e)
        }
        else {
            closure(this, e)
        }
    }


//
// futures
//

    def future(int delay = 0, boolean sync=false, Closure c) {
        sync ?
            plugin.server.scheduler.scheduleSyncDelayedTask(plugin, c, delay) :
            plugin.server.scheduler.scheduleAsyncDelayedTask(plugin, c, delay)
    }

    def repeat(int period, boolean sync=false, Closure c) {
        sync ?
            plugin.server.scheduler.scheduleSyncRepeatingTask(plugin, c, 0, period) :
            plugin.server.scheduler.scheduleAsyncRepeatingTask(plugin, c, 0, period)
    }


//
// commands
//

    boolean permitted(command) {
        plugin.permitted(player, command)
    }

	void command(String cmd, Closure c) {
        //println "$plugin.description.name registered command '$cmd'"
		plugin.commands[cmd] = c
	}

    def isCommand(command) {
        plugin.commands.containsKey(command)
    }

    def runCommand(command, args=null) {
        def closure = plugin.commands[command].clone()
        if (closure && permitted(command)) {
            if (player?.name != plugin.GROOVY_GOD) _log.info("${player?.name ?: 'server'}: $command> $args")
            closure.owner = this
            closure.delegate = this
            def result = closure(this, args)
            if (result) {
                if (player?.name != plugin.GROOVY_GOD) _log.info("${player?.name ?: 'server'}: $command< $result")
                if (player) player.sendMessage result.toString()
            }
        }
    }



//
// plugin updating
//

    def update(boolean force=false) {
        log("Updating GroovyBukkit (${plugin.description.version})")
        def pluginConfig = plugin.configuration

        def gbukkitLoc = pluginConfig.getString('update-location', 'https://github.com/krsmes/GroovyBukkit/zipball/master')
        def gbukkitDir = pluginConfig.getString('update-dir', 'plugins/update')
        def gbukkitJar = pluginConfig.getString('update-jar', 'GroovyBukkit.jar')
        def gbukkitLast = pluginConfig.getString('update-last')

        log("Checking: $gbukkitLoc")
        def conn = new URL(gbukkitLoc).openConnection()
        def filename = conn.headerFields.'Content-Disposition'[0].split('=')[1]

        // make sure file is different than previous
        if (filename == gbukkitLast && !force) {
            log("Already up-to-date ${plugin.description.version} ($filename)")
            return plugin.description.version
        }

        // download file
        def downloadFile = new File(plugin.tempFolder, filename)
        if (downloadFile.exists()) downloadFile.delete()
        downloadFile.withOutputStream { os ->  os << conn.inputStream }

        // open zip
        def downloadZip = new ZipFile(downloadFile)
        def zipRoot = filename - '.zip'

        // extract jar
        def updateJar = new File(gbukkitDir, gbukkitJar)
        if (updateJar.exists()) updateJar.delete()
        updateJar.withOutputStream { os ->
            os << downloadZip.getInputStream(downloadZip.getEntry(zipRoot + '/' + gbukkitJar))
        }

        // extract scripts
        def scriptsRoot = zipRoot + '/src/scripts/'
        def scriptsDir = new File(plugin.dataFolder, 'scripts/')
        scriptsDir.mkdirs()
        downloadZip.entries().findAll {it.name.startsWith(scriptsRoot)}.each {
            def name = it.name - scriptsRoot
            if (name && !name.endsWith('/')) {
                def scriptFile = new File(scriptsDir, name)
                //println name + " (${scriptFile.exists() ? 'exists' : 'new'}) ${it.size} bytes"
                scriptFile.withOutputStream { os -> os << downloadZip.getInputStream(it) }
            }
        }

        pluginConfig.setProperty('update-last', filename)
        log("Updated to $filename, reloading...")
        server.reload()
    }


    @Override protected void finalize() {
		println "${this}.finalize()"
		super.finalize()
	}

}