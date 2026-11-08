package org.helioviewer.jhv.thread;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import com.sun.jdi.Bootstrap;
import com.sun.jdi.VMDisconnectedException;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.connect.Connector;
import com.sun.jdi.connect.LaunchingConnector;
import com.sun.jdi.event.BreakpointEvent;
import com.sun.jdi.event.ClassPrepareEvent;
import com.sun.jdi.event.Event;
import com.sun.jdi.event.EventSet;
import com.sun.jdi.request.BreakpointRequest;
import com.sun.jdi.request.ClassPrepareRequest;
import com.sun.jdi.request.EventRequest;

public final class UiTimerTest {

    public static void main(String[] args) throws Exception {
        LaunchingConnector connector = Bootstrap.virtualMachineManager().defaultConnector();
        Map<String, Connector.Argument> launch = connector.defaultArguments();
        launch.get("main").setValue(Target.class.getName());
        launch.get("options").setValue("-cp \"" + System.getProperty("java.class.path") + "\"");
        VirtualMachine vm = connector.launch(launch);
        try {
            ClassPrepareRequest prepare = vm.eventRequestManager().createClassPrepareRequest();
            prepare.addClassFilter(UiTimer.class.getName());
            prepare.setSuspendPolicy(EventRequest.SUSPEND_ALL);
            prepare.enable();
            BufferedReader output = new BufferedReader(new InputStreamReader(vm.process().getInputStream()));
            PrintWriter input = new PrintWriter(vm.process().getOutputStream(), true);
            boolean restarted = false;
            while (!restarted) {
                EventSet events = vm.eventQueue().remove(15000);
                check(events != null, "No timer breakpoint event");
                for (Event event : events) {
                    if (event instanceof ClassPrepareEvent ready) {
                        BreakpointRequest breakpoint = vm.eventRequestManager().createBreakpointRequest(
                                ready.referenceType().methodsByName("tick").getFirst().location());
                        // Pause only the old scheduler tick; the UI owner must still restart the timer.
                        breakpoint.setSuspendPolicy(EventRequest.SUSPEND_EVENT_THREAD);
                        breakpoint.enable();
                    } else if (event instanceof BreakpointEvent stopped) {
                        stopped.request().disable();
                        check("STARTED".equals(output.readLine()), "Timer did not start");
                        input.println("restart");
                        check("RESTARTED".equals(output.readLine()), "Timer did not restart");
                        restarted = true;
                    }
                }
                events.resume();
            }
            check(vm.process().waitFor(15, TimeUnit.SECONDS), "Timer regression process did not finish");
            StringBuilder remaining = new StringBuilder();
            for (String line; (line = output.readLine()) != null;)
                remaining.append(line).append('\n');
            String errors = new String(vm.process().getErrorStream().readAllBytes());
            check(vm.process().exitValue() == 0, "Timer regression failed:\n" + remaining + errors);
            check(remaining.indexOf("PASS") >= 0, "Timer regression did not complete");
            System.out.print(remaining);
        } finally {
            try {
                vm.dispose();
            } catch (VMDisconnectedException ignored) {
            }
            vm.process().destroyForcibly();
        }
    }

    public static final class Target {
        private static final int DELAY = 3000;
        private static final LinkedBlockingQueue<Runnable> queue = new LinkedBlockingQueue<>();

        public static void main(String[] args) throws Exception {
            Thread owner = Thread.currentThread();
            UiThread.install(new UiThread.Impl() {
                @Override
                public void post(Runnable task) {
                    queue.add(task);
                }

                @Override
                public boolean isUiThread() {
                    return Thread.currentThread() == owner;
                }
            });
            int[] calls = {0};
            UiTimer timer = new UiTimer(DELAY, () -> {
                check(Thread.currentThread() == owner, "Callback left the UI owner");
                calls[0]++;
            });
            timer.setRepeats(false);
            try {
                timer.start();
                System.out.println("STARTED");
                BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
                check("restart".equals(input.readLine()), "Missing restart command");
                long restartedAt = System.nanoTime();
                timer.restart();
                System.out.println("RESTARTED");
                await().run();
                check(calls[0] == 0 && timer.isRunning(), "Old tick fired or stopped the restarted timer");
                check(System.nanoTime() - restartedAt < TimeUnit.MILLISECONDS.toNanos(DELAY),
                        "Old tick was not delivered before the restarted delay");
                await().run();
                check(calls[0] == 1 && !timer.isRunning(), "Restarted one-shot did not fire exactly once");
                check(System.nanoTime() - restartedAt >= TimeUnit.MILLISECONDS.toNanos(DELAY),
                        "Restarted timer fired before its delay");

                timer.setDelay(30);
                timer.start();
                Runnable queued = await();
                timer.restart();
                queued.run();
                check(calls[0] == 1 && timer.isRunning(), "Queued callback crossed restart");
                await().run();
                check(calls[0] == 2 && !timer.isRunning(), "Queued restart lost the new callback");

                timer.setRepeats(true);
                timer.start();
                queued = await();
                check(queue.poll(150, TimeUnit.MILLISECONDS) == null, "Ticks were not coalesced");
                queued.run();
                check(calls[0] == 3 && timer.isRunning(), "Repeating timer stopped after firing");
                queued = await();
                timer.stop();
                queued.run();
                check(calls[0] == 3 && !timer.isRunning(), "Stopped timer delivered a queued callback");
                System.out.println("PASS: in-flight and queued restart, one-shot delay, repeating coalescing and stop");
            } finally {
                timer.stop();
            }
        }

        private static Runnable await() throws InterruptedException {
            Runnable callback = queue.poll(5, TimeUnit.SECONDS);
            check(callback != null, "No queued timer callback");
            return callback;
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition)
            throw new AssertionError(message);
    }
}
