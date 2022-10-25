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
      rms: {
        options: {
          separator: ';'
        },
        src: ['app/userApp.js',
              'app/adminApp.js',
              'app/**/*.js',
             ],
        dest: 'build/app.min.js'
      
      },
      main: {
        options: {
          separator: ';'
        },
        src: ['config/config.js',
              'lib/jquery/jquery-1.8.2.min.js',
              'lib/jquery-ui/1.11.4/jquery-ui.min.js',
              'lib/angular/1.4.7/angular.min.js',
              'lib/angular/1.4.7/angular-sanitize.min.js',
              'lib/angular/1.4.7/angular-animate.js',
              'lib/angular/1.4.7/angular-messages.min.js',
              'lib/angular/1.4.7/angular-cookies.min.js',
              'lib/angular-dnd/2.1.0/angular-drag-and-drop-lists.min.js',
              'lib/angular-ui-router/0.2.15/angular-ui-router.min.js',
              'lib/angular-ui/bootstrap/ui-bootstrap-tpls-0.14.3.min.js',
              'lib/angular-ui-switch/angular-ui-switch.min.js',
              'lib/angular-translate/2.8.1/angular-translate.min.js',
              'lib/angular-translate/angular-translate-loader-static-files.min.js',
              'lib/jstree/3.2.1/js/jstree.min.js',
              'lib/tag-it/tag-it.min.js',
              'lib/ng-jstree/0.0.10/ngJsTree-custom.min.js',
              'lib/ng-file-upload/ng-file-upload.min.js',
              'lib/ng-resizable/angular-resizable.min.js',
              'lib/ngclipboard/clipboard.min.js',
              'lib/ngclipboard/ngclipboard.min.js',
              'lib/trNgGrid-3.1.5/trNgGrid.min.js',
              'lib/rms/rmsUtil.js',
              'lib/rms/clientDetector.js',
              'lib/hopscotch/js/hopscotch.min.js',
              'lib/3rdParty/js-joda.min.js',
              'lib/3rdParty/dateformat.js',
              'lib/3rdParty/moment.js',
              'lib/3rdParty/sha256.js',
              'build/templates.js',
              'build/app.min.js'
              ],
        dest: 'build/main.min.js'
      },
      login: {
        options: {
          separator: ';'
        },
        src: ['lib/rms/clientDetector.js',
              'lib/rms/rmsUtil.js',
              'lib/rms/login.js',
             ],
        dest: 'build/login.min.js'
      },
      'login-css': {
        options: {
          separator: ''
        },
        src: ['lib/bootstrap-partial/css/bootstrap.min.css',
              'css/login.css',
             ],
        dest: 'build/login.min.css'
      },
      'main-css': {
        options: {
          separator: ''
        },
        src: ['lib/angular-ui-switch/angular-ui-switch.css',
              'lib/ng-resizable/angular-resizable.min.css',
              'lib/tag-it/css/jquery.tagit.css',
              'lib/tag-it/css/tagit.ui-zendesk.css',
              'lib/trNgGrid-3.1.5/trNgGrid.min.css',
              'lib/hopscotch/css/hopscotch.min.css',
              'css/style.css',
              'css/jstreestyle.css',
             ],
        dest: 'build/main.min.css'
      }
    },
    uglify: {
      options: {
        banner: ''
      },
      main: {
        src: 'build/main.min.js',
        dest: 'build/deploy-main.js'
      },
      login: {
        src: 'build/login.min.js',
        dest: 'build/deploy-login.js'
      }
    },
    cssmin: {
      login: {
        src: 'build/login.min.css',
        dest: 'build/deploy-login.css'
      },
      main: {
        src: 'build/main.min.css',
        dest: 'build/deploy-main.css'
      }
    },
    copy: {
      main: {
        files: [
          // includes files within path and its sub-directories
          {flatten: true, src: ['build/deploy-main.js'], dest: 'deploy/ui/app/main.min.js',filter:'isFile'},
          {flatten: true, src: ['build/deploy-login.js'], dest: 'deploy/ui/app/login.min.js',filter:'isFile'},
          {flatten: true, src: ['build/deploy-login.css'], dest: 'deploy/ui/css/login.min.css',filter:'isFile'},
          {flatten: true, src: ['build/deploy-main.css'], dest: 'deploy/ui/css/main.min.css',filter:'isFile'},
          {expand: true, src: ['lib/**'], dest: 'deploy/ui/'},
          {expand: true, src: ['css/**'], dest: 'deploy/ui/'},
          {expand: true, src: ['img/**'], dest: 'deploy/ui/'},
          {flatten: true, src: ['app/i18n/*.json'], dest: 'deploy/ui/'},
          {expand: true, src: ['../tenants/**'], dest: 'deploy/ui/'},
          {flatten: true, src: ['../main.jsp'], dest: 'deploy/main.jsp', filter:'isFile'},
          {flatten: true, src: ['../Login.jsp'], dest: 'deploy/Login.jsp', filter:'isFile'},
          {flatten: true, src: ['../LoginAdmin.jsp'], dest: 'deploy/LoginAdmin.jsp', filter:'isFile'},
          {flatten: true, src: ['../Intro.jsp'], dest: 'deploy/Intro.jsp', filter:'isFile'},
          {flatten: true, src: ['../OpenLink.jsp'], dest: 'deploy/OpenLink.jsp', filter:'isFile'},
          {flatten: true, src: ['../SharepointApp.jsp'], dest: 'deploy/SharepointApp.jsp', filter:'isFile'},
          {flatten: true, src: ['../ShowError.jsp'], dest: 'deploy/ShowError.jsp', filter:'isFile'},
          {flatten: true, src: ['../CustomURL.jsp'], dest: 'deploy/CustomURL.jsp', filter:'isFile'},
          {flatten: true, src: ['../TimeOut.jsp'], dest: 'deploy/TimeOut.jsp', filter:'isFile'},
          {flatten: true, src: ['../error_404.jsp'], dest: 'deploy/error_404.jsp', filter:'isFile'},
          {flatten: true, src: ['../error_403.jsp'], dest: 'deploy/error_403.jsp', filter:'isFile'},
          {flatten: true, src: ['../error_500.jsp'], dest: 'deploy/error_500.jsp', filter:'isFile'},
          {flatten: true, src: ['../TenantResolver.jsp'], dest: 'deploy/TenantResolver.jsp', filter:'isFile'},
          {flatten: true, src: ['../LogoWrapper.jsp'], dest: 'deploy/LogoWrapper.jsp', filter:'isFile'},
          {flatten: true, src: ['../Register.jsp'], dest: 'deploy/Register.jsp', filter:'isFile'},
          {flatten: true, src: ['../ForgotPassword.jsp'], dest: 'deploy/ForgotPassword.jsp', filter:'isFile'},
          {flatten: true, src: ['../ResetPassword.jsp'], dest: 'deploy/ResetPassword.jsp', filter:'isFile'},
          {flatten: true, src: ['../Footer.jsp'], dest: 'deploy/Footer.jsp', filter:'isFile'},
          {flatten: true, src: ['../activate.jsp'], dest: 'deploy/activate.jsp', filter:'isFile'},
          {flatten: true, src: ['../unregister.jsp'], dest: 'deploy/unregister.jsp', filter:'isFile'},
          {flatten: true, src: ['../PrivacyPolicy.html'], dest: 'deploy/PrivacyPolicy.html', filter:'isFile'},
          {flatten: true, src: ['../TermsAndConditions.html'], dest: 'deploy/TermsAndConditions.html', filter:'isFile'},
          {flatten: true, src: ['../data-processing-addendum-dpa.html'], dest: 'deploy/data-processing-addendum-dpa.html', filter:'isFile'},
          {flatten: true, src: ['../ProjectPrivacyPolicy.html'], dest: 'deploy/ProjectPrivacyPolicy.html', filter:'isFile'},
          {flatten: true, src: ['../ProjectTermsAndConditions.html'], dest: 'deploy/ProjectTermsAndConditions.html', filter:'isFile'},
          {flatten: true, src: ['../rms-logo.png'], dest: 'deploy/rms-logo.png', filter:'isFile'},
          {flatten: true, src: ['../rms-logo-with-text.png'], dest: 'deploy/rms-logo-with-text.png', filter:'isFile'},
          {flatten: true, src: ['../invitation.jsp'], dest: 'deploy/invitation.jsp', filter:'isFile'},
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
  grunt.registerTask('default', ['clean','html2js','concat','uglify','cssmin','copy']);
};
