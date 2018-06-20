module.exports = function(grunt) {

  // Project configuration.
  grunt.initConfig({
    pkg: grunt.file.readJSON('package.json'),
    uglify: {
		js: {
			files :{
				'cxopcollector.js': [
				'Boomerang/*.js',
				'Boomerang/Plugins/rt.js',
                'Boomerang/Plugins/restiming.js',
				'Boomerang/Plugins/spa.js',
				'Boomerang/Plugins/angular.js',
                'Boomerang/Plugins/auto-xhr.js',
                'Boomerang/Plugins/memory.js',
                'Boomerang/Plugins/navtiming.js',
                'Boomerang/Plugins/cxoptimize.js',
                'Boomerang/Plugins/cxoptimizeconfig.js'
				]
			}
		},
      options: {
        banner: '/*! <%= pkg.name %> <%= grunt.template.today("yyyy-mm-dd") %> */\n'
      },
      build: {
      }
    }
  });

  // Load the plugin that provides the "uglify" task.
  grunt.loadNpmTasks('grunt-contrib-uglify');

  // Default task(s).
  grunt.registerTask('default', ['uglify:js']);

};