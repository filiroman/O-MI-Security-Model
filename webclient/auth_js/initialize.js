(function() {
    $(function() {
      $('#btn-fblogin').on('click', function() {
        window.location.href = "/security/Login?auth_type=facebook";
      });

      $('#btn-signup').on('click', function() {

        var pass1 = $('#password_first').val();
        var pass2 = $('#password_second').val();

        console.log(pass1);
        console.log(pass2);

        if (pass1 != pass2)
        {
          alert("Passwords do not match!");
        } else {
          $('#signupform').submit();
        }
      });

      $('#btn-login').on('click', function() {
        $('#loginform').submit();
      });
      // my.odfTreeDom = $('#nodetree');
    });

}).call(this);
