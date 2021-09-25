(ns formatting-stack.plugin
  "These hooks can be used to hook into parts of the formatting-stack process.
  The hooks are executed by formatting-stack in order of definition.

  Plugins should be defined in one namespace, and can partially implement hooks.")

(defmulti cli-options
  "Return value is merged into cli option specs (compatible with tools.cli)."
  identity)

(defmulti config
  "Return value is merged into the context"
  (fn [id _context] id))

(defmulti post-config
  "return value is merged into context"
  (fn [id _context] id))

(defmulti pre-process
  "return value is merged into context"
  (fn [id _context] id))

(defmulti process
  "return value is deep-merged into context"
  (fn [id _context] id))

(defmulti post-process
  "return value is merged into context"
  (fn [id _context] id))
