package ru.hukm.effectiveSpigot.interfaces

/**
 * A framework subsystem with a one-time [init] hook, called once during EffectiveSpigot startup.
 * Each `Effective*` feature exposes its module via a `getModule()`; they are collected and initialized
 * together when the framework enables.
 *
 * @suppress internal framework wiring — hidden from the public docs.
 */
interface IModule {
    /** Runs the module's one-time setup (usually registering event listeners). */
    fun init()
}