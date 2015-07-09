package es.uvigo.det.labredes.dualeee;

/**
 * All the possible states of the EEE link.
 */
public enum EeeState {
    /**
     * The link is active.
     */
    ACTIVE,
	/**
	 * The link is in the fast wake state (clock only, fast recovery mode).
	 */
	FAST_WAKE,
	/**
	 * The link is in the deep sleep state (clock stopped, slow recovery mode).
	 */
	DEEP_SLEEP,
	/**
	 * The link is transitioning to active from the fast wake state.
	 */
	TRANSITION_TO_ACTIVE_FROM_FAST,
	/**
	 * The link is transitioning to active from the deep sleep state.
	 */
	TRANSITION_TO_ACTIVE_FROM_DEEP,
	/**
	 * The link is transitioning to the fast wake state.
	 */
	TRANSITION_TO_FAST,
	/**
	 * The link is transitioning to the deep sleep state.
	 */
	TRANSITION_TO_DEEP;
}
