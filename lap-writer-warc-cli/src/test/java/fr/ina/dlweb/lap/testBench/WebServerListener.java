package fr.ina.dlweb.lap.testBench;

import org.mortbay.component.LifeCycle;

/**
 * Date: 23/11/12
 * Time: 11:37
 *
 * @author drapin
 */
public interface WebServerListener {
    void onStopped(LifeCycle lifeCycle);

    void onStarted(LifeCycle lifeCycle);
}
