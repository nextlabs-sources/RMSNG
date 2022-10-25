module.exports = function(grunt) {

  // Project configuration.
  grunt.initConfig({
    pkg: grunt.file.readJSON('package.json'),
    clean: {
      build: ['build'],
      deploy: {
         options: { force: true },
         src: ['deploy']
      } 
    },
    rmsfolder: "../../../rms/server/web/ui",
    html2js: {
      options: {
        // custom options
        base:'../'
      },
      main: {
        src: ['app/**/*.html'],
        dest: 'build/templates.js'
      },
    },
    concat: {
      viewer: {
        options: {
          separator: ';'
        },
        src: ['app/app.js',
              '../../../rms/server/web/ui/app/**/*.js',
              '!../../../rms/server/web/ui/app/app.js',
              '!../../../rms/server/web/ui/app/userApp.js',
              '!../../../rms/server/web/ui/app/adminApp.js'
             ],
        dest: 'build/viewer.app.min.js'
      
      },
      viewerMain: {
        options: {
          separator: ';'
        },
        src: [
              '<%= rmsfolder %>/lib/angular/1.4.7/angular.min.js',
              '<%= rmsfolder %>/lib/angular/1.4.7/angular-sanitize.min.js',
              '<%= rmsfolder %>/lib/angular/1.4.7/angular-animate.js',
              '<%= rmsfolder %>/lib/angular/1.4.7/angular-messages.min.js',
              '<%= rmsfolder %>/lib/angular/1.4.7/angular-cookies.min.js',
              '<%= rmsfolder %>/lib/angular-ui-router/0.2.15/angular-ui-router.min.js',
              '<%= rmsfolder %>/lib/angular-ui/bootstrap/ui-bootstrap-tpls-0.14.3.min.js',
              '<%= rmsfolder %>/lib/angular-ui-switch/angular-ui-switch.min.js',
              '<%= rmsfolder %>/lib/angular-translate/2.8.1/angular-translate.min.js',
              '<%= rmsfolder %>/lib/angular-translate/angular-translate-loader-static-files.min.js',
              '<%= rmsfolder %>/lib/jstree/3.2.1/js/jstree.min.js',
              '<%= rmsfolder %>/lib/tag-it/tag-it.min.js',
              '<%= rmsfolder %>/lib/ng-jstree/0.0.10/ngJsTree-custom.min.js',
              '<%= rmsfolder %>/lib/ng-file-upload/ng-file-upload.min.js',
              '<%= rmsfolder %>/lib/ng-resizable/angular-resizable.min.js',
              '<%= rmsfolder %>/lib/ngclipboard/clipboard.min.js',
              '<%= rmsfolder %>/lib/ngclipboard/ngclipboard.min.js',
              '<%= rmsfolder %>/lib/trNgGrid-3.1.5/trNgGrid.min.js',
              '<%= rmsfolder %>/lib/rms/clientDetector.js',
              '<%= rmsfolder %>/lib/hopscotch/js/hopscotch.min.js',
              'build/templates.js',
              'build/viewer.app.min.js'
             ],
        dest: 'build/viewer.min.js'
      },
    },
    uglify: {
      options: {
        banner: ''
      },
      viewerMain: {
        src: 'build/viewer.min.js',
        dest: 'build/deploy-viewer.js'
      }
    },
    copy: {
      main: {
        files: [
          // includes files within path and its sub-directories
          {flatten: true, src: ['build/deploy-viewer.js'], dest: 'app/viewers/viewer.min.js',filter:'isFile'},
          {flatten: true, src: ['<%= rmsfolder %>/app/i18n/en.json'], dest: 'app/i18n/rms_en.json',filter:'isFile'},
        ]
      }
    }
  });

  // Load the plugin that provides the "uglify" task.
  grunt.loadNpmTasks('grunt-contrib-uglify');
  grunt.loadNpmTasks('grunt-html2js');
  grunt.loadNpmTasks('grunt-contrib-concat');
  grunt.loadNpmTasks('grunt-contrib-copy');
  grunt.loadNpmTasks('grunt-contrib-clean');
  grunt.loadNpmTasks('grunt-css');

  // Default task(s).
  grunt.registerTask('default', ['clean','html2js','concat','uglify','copy','clean']);
};
