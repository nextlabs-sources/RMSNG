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
      }
    },
    uglify: {
      options: {
        banner: ''
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
      }
    },
    copy: {
      main: {
        files: [
          // includes files within path and its sub-directories
          {flatten: true, src: ['build/deploy-login.js'], dest: 'deploy/ui/app/login.min.js',filter:'isFile'},
          {flatten: true, src: ['build/deploy-login.css'], dest: 'deploy/ui/css/login.min.css',filter:'isFile'},
          {flatten: true, src: ['build/templates.js'], dest: 'deploy/ui/app/templates.js',filter:'isFile'},
          {expand: true, src: ['app/**'], dest: 'deploy/ui/'},
          {expand: true, src: ['lib/**'], dest: 'deploy/ui/'},
          {expand: true, src: ['css/**'], dest: 'deploy/ui/'},
          {expand: true, src: ['img/**'], dest: 'deploy/ui/'}, 
          {flatten: true, src: ['app/i18n/*.json'], dest: 'deploy/ui/'},
          {expand: true, src: ['../tenants/**'], dest: 'deploy/ui/'},
          {flatten: true, src: ['../main-dev.jsp'], dest: 'deploy/main.jsp', filter:'isFile'},
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
