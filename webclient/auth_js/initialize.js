(function() {
    $(function() {
      $('#btn-fblogin').on('click', function() {
        window.location.href = "../Login?auth_type=facebook";
      });

      $('#btn-signup').on('click', function() {

        var pass1 = $('input[name=passwd]').val();
        var pass2 = $('input[name=passwd_repeat]').val();

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
