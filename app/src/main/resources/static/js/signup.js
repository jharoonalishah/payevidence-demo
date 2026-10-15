(function () {
  document.querySelectorAll("form.signup-form").forEach(function (form) {
    form.addEventListener("submit", function (event) {
      var btn = form.querySelector("[data-signup-submit]");
      if (!btn) return;
      if (btn.dataset.submitted === "1") {
        event.preventDefault();
        return;
      }
      btn.dataset.submitted = "1";
      btn.setAttribute("aria-busy", "true");
      btn.textContent = "Sending…";
      setTimeout(function () {
        btn.disabled = true;
      }, 0);
    });
  });
})();
