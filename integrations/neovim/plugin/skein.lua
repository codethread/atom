if vim.g.loaded_skein_nvim then
  return
end
vim.g.loaded_skein_nvim = true

vim.api.nvim_create_user_command("SkeinConnect", function(opts)
  require("skein").connect(opts)
end, {
  desc = "Select a running Skein weaver and connect Conjure to its nREPL",
})
