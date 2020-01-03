Isolated project reproducing `IllegalReferenceCountException` error corrupting
the `reactor-netty` state. Below are the steps to follow:

1. Use a recent JDK:

   ```bash
   $ java -version
   openjdk version "13.0.1" 2019-10-15
   OpenJDK Runtime Environment AdoptOpenJDK (build 13.0.1+9)
   OpenJDK 64-Bit Server VM AdoptOpenJDK (build 13.0.1+9, mixed mode, sharing)
   ```

2. Compile the sources:

   ```bash
   $ mvn clean compile
   ```

3. Run the server with a relatively large response payload:

   ```bash
   $ MAVEN_OPTS="-XX:+TieredCompilation -Xmx1g" \
       taskset -c 0 \
       mvn exec:java \
       -Dexec.mainClass=com.vlkan.workbench.BenchmarkServer \
       -Dbenchmark.responsePayloadLength=$[1000 * 16 * 1024]
   [INFO] Scanning for projects...
   [INFO]
   [INFO] -----------------< com.vlkan:reactor-netty-workbench >------------------
   [INFO] Building reactor-netty-workbench 1.0-SNAPSHOT
   [INFO] --------------------------------[ jar ]---------------------------------
   [INFO]
   [INFO] --- exec-maven-plugin:1.6.0:java (default-cli) @ reactor-netty-workbench ---
   20:43:14.327 [com.vlkan.workbench.BenchmarkServer.main()] INFO  com.vlkan.workbench.BenchmarkServer - host = localhost
   20:43:14.329 [com.vlkan.workbench.BenchmarkServer.main()] INFO  com.vlkan.workbench.BenchmarkServer - port = 7777
   20:43:14.329 [com.vlkan.workbench.BenchmarkServer.main()] INFO  com.vlkan.workbench.BenchmarkServer - responsePayloadLength = 16384000
   20:43:14.329 [com.vlkan.workbench.BenchmarkServer.main()] INFO  com.vlkan.workbench.BenchmarkServer - building response payloads
   20:43:14.421 [com.vlkan.workbench.BenchmarkServer.main()] INFO  com.vlkan.workbench.BenchmarkServer - starting server
   20:43:14.759 [com.vlkan.workbench.BenchmarkServer.main()] INFO  com.vlkan.workbench.BenchmarkServer - started server
   ```

4. Run the client where the concurrency is bigger than the available cores:

   ```bash
   MAVEN_OPTS="-XX:+TieredCompilation -Xmx1g" \
       taskset -c 1 \
       mvn exec:java \
       -Dexec.mainClass=com.vlkan.workbench.BenchmarkClient \
       -Dbenchmark.concurrency=3 \
       -Dbenchmark.periodSecs=10
   [INFO] Scanning for projects...
   [INFO] 
   [INFO] -----------------< com.vlkan:reactor-netty-workbench >------------------
   [INFO] Building reactor-netty-workbench 1.0-SNAPSHOT
   [INFO] --------------------------------[ jar ]---------------------------------
   [INFO] 
   [INFO] --- exec-maven-plugin:1.6.0:java (default-cli) @ reactor-netty-workbench ---
   20:49:32.163 [com.vlkan.workbench.BenchmarkClient.main()] INFO  com.vlkan.workbench.BenchmarkClient - baseUrl = http://localhost:7777
   20:49:32.165 [com.vlkan.workbench.BenchmarkClient.main()] INFO  com.vlkan.workbench.BenchmarkClient - concurrency = 3
   20:49:32.165 [com.vlkan.workbench.BenchmarkClient.main()] INFO  com.vlkan.workbench.BenchmarkClient - periodSecs = 10
   20:49:32.166 [com.vlkan.workbench.BenchmarkClient.main()] INFO  com.vlkan.workbench.BenchmarkClient - starting
   20:49:42.418 [parallel-1] ERROR reactor.core.publisher.Operators - Operator called default onErrorDropped
   io.netty.util.IllegalReferenceCountException: refCnt: 0, decrement: 1
   	at io.netty.util.internal.ReferenceCountUpdater.toLiveRealRefCnt(ReferenceCountUpdater.java:74) ~[netty-common-4.1.43.Final.jar:4.1.43.Final]
   	at io.netty.util.internal.ReferenceCountUpdater.release(ReferenceCountUpdater.java:138) ~[netty-common-4.1.43.Final.jar:4.1.43.Final]
   	at io.netty.buffer.AbstractReferenceCountedByteBuf.release(AbstractReferenceCountedByteBuf.java:100) ~[netty-buffer-4.1.43.Final.jar:4.1.43.Final]
   	at io.netty.buffer.CompositeByteBuf$Component.free(CompositeByteBuf.java:1915) ~[netty-buffer-4.1.43.Final.jar:4.1.43.Final]
   	at io.netty.buffer.CompositeByteBuf.deallocate(CompositeByteBuf.java:2220) ~[netty-buffer-4.1.43.Final.jar:4.1.43.Final]
   	at io.netty.buffer.AbstractReferenceCountedByteBuf.handleRelease(AbstractReferenceCountedByteBuf.java:110) ~[netty-buffer-4.1.43.Final.jar:4.1.43.Final]
   	at io.netty.buffer.AbstractReferenceCountedByteBuf.release(AbstractReferenceCountedByteBuf.java:100) ~[netty-buffer-4.1.43.Final.jar:4.1.43.Final]
   	at reactor.netty.ByteBufFlux.lambda$aggregate$8(ByteBufFlux.java:286) ~[reactor-netty-0.9.2.RELEASE.jar:0.9.2.RELEASE]
   	at reactor.core.publisher.FluxDoFinally$DoFinallySubscriber.runFinally(FluxDoFinally.java:156) [reactor-core-3.3.1.RELEASE.jar:3.3.1.RELEASE]
   	at reactor.core.publisher.FluxDoFinally$DoFinallySubscriber.cancel(FluxDoFinally.java:145) [reactor-core-3.3.1.RELEASE.jar:3.3.1.RELEASE]
   	at reactor.core.publisher.FluxMap$MapConditionalSubscriber.cancel(FluxMap.java:286) [reactor-core-3.3.1.RELEASE.jar:3.3.1.RELEASE]
   	at reactor.core.publisher.FluxHandle$HandleSubscriber.cancel(FluxHandle.java:259) [reactor-core-3.3.1.RELEASE.jar:3.3.1.RELEASE]
   	at reactor.core.publisher.FluxMap$MapSubscriber.cancel(FluxMap.java:160) [reactor-core-3.3.1.RELEASE.jar:3.3.1.RELEASE]
   	at reactor.core.publisher.FluxOnAssembly$OnAssemblySubscriber.cancel(FluxOnAssembly.java:500) [reactor-core-3.3.1.RELEASE.jar:3.3.1.RELEASE]
   	at reactor.core.publisher.FluxDoFinally$DoFinallySubscriber.cancel(FluxDoFinally.java:144) [reactor-core-3.3.1.RELEASE.jar:3.3.1.RELEASE]
   	at reactor.core.publisher.Operators.terminate(Operators.java:1108) [reactor-core-3.3.1.RELEASE.jar:3.3.1.RELEASE]
   	at reactor.core.publisher.MonoFlatMap$FlatMapInner.cancel(MonoFlatMap.java:264) [reactor-core-3.3.1.RELEASE.jar:3.3.1.RELEASE]
   	at reactor.core.publisher.MonoFlatMap$FlatMapMain.cancel(MonoFlatMap.java:181) [reactor-core-3.3.1.RELEASE.jar:3.3.1.RELEASE]
   	at reactor.core.publisher.FluxOnAssembly$OnAssemblySubscriber.cancel(FluxOnAssembly.java:500) [reactor-core-3.3.1.RELEASE.jar:3.3.1.RELEASE]
   	at reactor.core.publisher.Operators.terminate(Operators.java:1108) [reactor-core-3.3.1.RELEASE.jar:3.3.1.RELEASE]
   	at reactor.core.publisher.FluxFlatMap$FlatMapInner.cancel(FluxFlatMap.java:1008) [reactor-core-3.3.1.RELEASE.jar:3.3.1.RELEASE]
   	at reactor.core.publisher.FluxFlatMap$FlatMapMain.unsubscribeEntry(FluxFlatMap.java:332) [reactor-core-3.3.1.RELEASE.jar:3.3.1.RELEASE]
   	at reactor.core.publisher.FluxFlatMap$FlatMapMain.unsubscribeEntry(FluxFlatMap.java:212) [reactor-core-3.3.1.RELEASE.jar:3.3.1.RELEASE]
   	at reactor.core.publisher.FlatMapTracker.unsubscribe(FluxFlatMap.java:1068) [reactor-core-3.3.1.RELEASE.jar:3.3.1.RELEASE]
   	at reactor.core.publisher.FluxFlatMap$FlatMapMain.cancel(FluxFlatMap.java:352) [reactor-core-3.3.1.RELEASE.jar:3.3.1.RELEASE]
   	at reactor.core.publisher.FluxOnAssembly$OnAssemblySubscriber.cancel(FluxOnAssembly.java:500) [reactor-core-3.3.1.RELEASE.jar:3.3.1.RELEASE]
   	at reactor.core.publisher.FluxPeekFuseable$PeekFuseableSubscriber.cancel(FluxPeekFuseable.java:152) [reactor-core-3.3.1.RELEASE.jar:3.3.1.RELEASE]
   	at reactor.core.publisher.Operators$MultiSubscriptionSubscriber.drainLoop(Operators.java:2051) [reactor-core-3.3.1.RELEASE.jar:3.3.1.RELEASE]
   	at reactor.core.publisher.Operators$MultiSubscriptionSubscriber.drain(Operators.java:2020) [reactor-core-3.3.1.RELEASE.jar:3.3.1.RELEASE]
   	at reactor.core.publisher.Operators$MultiSubscriptionSubscriber.cancel(Operators.java:1832) [reactor-core-3.3.1.RELEASE.jar:3.3.1.RELEASE]
   	at reactor.core.publisher.FluxTakeUntilOther$TakeUntilMainSubscriber.cancelMain(FluxTakeUntilOther.java:173) [reactor-core-3.3.1.RELEASE.jar:3.3.1.RELEASE]
   	at reactor.core.publisher.FluxTakeUntilOther$TakeUntilMainSubscriber.cancel(FluxTakeUntilOther.java:190) [reactor-core-3.3.1.RELEASE.jar:3.3.1.RELEASE]
   	at reactor.core.publisher.FluxTakeUntilOther$TakeUntilMainSubscriber.onComplete(FluxTakeUntilOther.java:234) [reactor-core-3.3.1.RELEASE.jar:3.3.1.RELEASE]
   	at reactor.core.publisher.FluxTakeUntilOther$TakeUntilOtherSubscriber.onComplete(FluxTakeUntilOther.java:112) [reactor-core-3.3.1.RELEASE.jar:3.3.1.RELEASE]
   	at reactor.core.publisher.FluxTakeUntilOther$TakeUntilOtherSubscriber.onNext(FluxTakeUntilOther.java:94) [reactor-core-3.3.1.RELEASE.jar:3.3.1.RELEASE]
   	at reactor.core.publisher.MonoDelay$MonoDelayRunnable.run(MonoDelay.java:117) [reactor-core-3.3.1.RELEASE.jar:3.3.1.RELEASE]
   	at reactor.core.scheduler.SchedulerTask.call(SchedulerTask.java:68) [reactor-core-3.3.1.RELEASE.jar:3.3.1.RELEASE]
   	at reactor.core.scheduler.SchedulerTask.call(SchedulerTask.java:28) [reactor-core-3.3.1.RELEASE.jar:3.3.1.RELEASE]
   	at java.util.concurrent.FutureTask.run(FutureTask.java:264) [?:?]
   	at java.util.concurrent.ScheduledThreadPoolExecutor$ScheduledFutureTask.run(ScheduledThreadPoolExecutor.java:304) [?:?]
   	at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1128) [?:?]
   	at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:628) [?:?]
   	at java.lang.Thread.run(Thread.java:830) [?:?]
   20:49:42.453 [parallel-1] ERROR reactor.core.scheduler.Schedulers - Scheduler worker in group com.vlkan.workbench.BenchmarkClient failed with an uncaught exception
   io.netty.util.IllegalReferenceCountException: refCnt: 0, decrement: 1
   	at io.netty.util.internal.ReferenceCountUpdater.toLiveRealRefCnt(ReferenceCountUpdater.java:74) ~[netty-common-4.1.43.Final.jar:4.1.43.Final]
   	at io.netty.util.internal.ReferenceCountUpdater.release(ReferenceCountUpdater.java:138) ~[netty-common-4.1.43.Final.jar:4.1.43.Final]
   	at io.netty.buffer.AbstractReferenceCountedByteBuf.release(AbstractReferenceCountedByteBuf.java:100) ~[netty-buffer-4.1.43.Final.jar:4.1.43.Final]
   	at io.netty.buffer.CompositeByteBuf$Component.free(CompositeByteBuf.java:1915) ~[netty-buffer-4.1.43.Final.jar:4.1.43.Final]
   	at io.netty.buffer.CompositeByteBuf.deallocate(CompositeByteBuf.java:2220) ~[netty-buffer-4.1.43.Final.jar:4.1.43.Final]
   	at io.netty.buffer.AbstractReferenceCountedByteBuf.handleRelease(AbstractReferenceCountedByteBuf.java:110) ~[netty-buffer-4.1.43.Final.jar:4.1.43.Final]
   	at io.netty.buffer.AbstractReferenceCountedByteBuf.release(AbstractReferenceCountedByteBuf.java:100) ~[netty-buffer-4.1.43.Final.jar:4.1.43.Final]
   	at reactor.netty.ByteBufFlux.lambda$aggregate$8(ByteBufFlux.java:286) ~[reactor-netty-0.9.2.RELEASE.jar:0.9.2.RELEASE]
   	at reactor.core.publisher.FluxDoFinally$DoFinallySubscriber.runFinally(FluxDoFinally.java:156) ~[reactor-core-3.3.1.RELEASE.jar:3.3.1.RELEASE]
   	at reactor.core.publisher.FluxDoFinally$DoFinallySubscriber.cancel(FluxDoFinally.java:145) ~[reactor-core-3.3.1.RELEASE.jar:3.3.1.RELEASE]
   	at reactor.core.publisher.FluxMap$MapConditionalSubscriber.cancel(FluxMap.java:286) ~[reactor-core-3.3.1.RELEASE.jar:3.3.1.RELEASE]
   	at reactor.core.publisher.FluxHandle$HandleSubscriber.cancel(FluxHandle.java:259) ~[reactor-core-3.3.1.RELEASE.jar:3.3.1.RELEASE]
   	at reactor.core.publisher.FluxMap$MapSubscriber.cancel(FluxMap.java:160) ~[reactor-core-3.3.1.RELEASE.jar:3.3.1.RELEASE]
   	at reactor.core.publisher.FluxOnAssembly$OnAssemblySubscriber.cancel(FluxOnAssembly.java:500) ~[reactor-core-3.3.1.RELEASE.jar:3.3.1.RELEASE]
   	at reactor.core.publisher.FluxDoFinally$DoFinallySubscriber.cancel(FluxDoFinally.java:144) ~[reactor-core-3.3.1.RELEASE.jar:3.3.1.RELEASE]
   	at reactor.core.publisher.Operators.terminate(Operators.java:1108) ~[reactor-core-3.3.1.RELEASE.jar:3.3.1.RELEASE]
   	at reactor.core.publisher.MonoFlatMap$FlatMapInner.cancel(MonoFlatMap.java:264) ~[reactor-core-3.3.1.RELEASE.jar:3.3.1.RELEASE]
   	at reactor.core.publisher.MonoFlatMap$FlatMapMain.cancel(MonoFlatMap.java:181) ~[reactor-core-3.3.1.RELEASE.jar:3.3.1.RELEASE]
   	at reactor.core.publisher.FluxOnAssembly$OnAssemblySubscriber.cancel(FluxOnAssembly.java:500) ~[reactor-core-3.3.1.RELEASE.jar:3.3.1.RELEASE]
   	at reactor.core.publisher.Operators.terminate(Operators.java:1108) ~[reactor-core-3.3.1.RELEASE.jar:3.3.1.RELEASE]
   	at reactor.core.publisher.FluxFlatMap$FlatMapInner.cancel(FluxFlatMap.java:1008) ~[reactor-core-3.3.1.RELEASE.jar:3.3.1.RELEASE]
   	at reactor.core.publisher.FluxFlatMap$FlatMapMain.unsubscribeEntry(FluxFlatMap.java:332) ~[reactor-core-3.3.1.RELEASE.jar:3.3.1.RELEASE]
   	at reactor.core.publisher.FluxFlatMap$FlatMapMain.unsubscribeEntry(FluxFlatMap.java:212) ~[reactor-core-3.3.1.RELEASE.jar:3.3.1.RELEASE]
   	at reactor.core.publisher.FlatMapTracker.unsubscribe(FluxFlatMap.java:1068) ~[reactor-core-3.3.1.RELEASE.jar:3.3.1.RELEASE]
   	at reactor.core.publisher.FluxFlatMap$FlatMapMain.cancel(FluxFlatMap.java:352) ~[reactor-core-3.3.1.RELEASE.jar:3.3.1.RELEASE]
   	at reactor.core.publisher.FluxOnAssembly$OnAssemblySubscriber.cancel(FluxOnAssembly.java:500) ~[reactor-core-3.3.1.RELEASE.jar:3.3.1.RELEASE]
   	at reactor.core.publisher.FluxPeekFuseable$PeekFuseableSubscriber.cancel(FluxPeekFuseable.java:152) ~[reactor-core-3.3.1.RELEASE.jar:3.3.1.RELEASE]
   	at reactor.core.publisher.Operators$MultiSubscriptionSubscriber.drainLoop(Operators.java:2051) ~[reactor-core-3.3.1.RELEASE.jar:3.3.1.RELEASE]
   	at reactor.core.publisher.Operators$MultiSubscriptionSubscriber.drain(Operators.java:2020) ~[reactor-core-3.3.1.RELEASE.jar:3.3.1.RELEASE]
   	at reactor.core.publisher.Operators$MultiSubscriptionSubscriber.cancel(Operators.java:1832) ~[reactor-core-3.3.1.RELEASE.jar:3.3.1.RELEASE]
   	at reactor.core.publisher.FluxTakeUntilOther$TakeUntilMainSubscriber.cancelMain(FluxTakeUntilOther.java:173) ~[reactor-core-3.3.1.RELEASE.jar:3.3.1.RELEASE]
   	at reactor.core.publisher.FluxTakeUntilOther$TakeUntilMainSubscriber.cancel(FluxTakeUntilOther.java:190) ~[reactor-core-3.3.1.RELEASE.jar:3.3.1.RELEASE]
   	at reactor.core.publisher.FluxTakeUntilOther$TakeUntilMainSubscriber.onComplete(FluxTakeUntilOther.java:234) ~[reactor-core-3.3.1.RELEASE.jar:3.3.1.RELEASE]
   	at reactor.core.publisher.FluxTakeUntilOther$TakeUntilOtherSubscriber.onComplete(FluxTakeUntilOther.java:112) ~[reactor-core-3.3.1.RELEASE.jar:3.3.1.RELEASE]
   	at reactor.core.publisher.FluxTakeUntilOther$TakeUntilOtherSubscriber.onNext(FluxTakeUntilOther.java:94) ~[reactor-core-3.3.1.RELEASE.jar:3.3.1.RELEASE]
   	at reactor.core.publisher.MonoDelay$MonoDelayRunnable.run(MonoDelay.java:117) ~[reactor-core-3.3.1.RELEASE.jar:3.3.1.RELEASE]
   	at reactor.core.scheduler.SchedulerTask.call(SchedulerTask.java:68) [reactor-core-3.3.1.RELEASE.jar:3.3.1.RELEASE]
   	at reactor.core.scheduler.SchedulerTask.call(SchedulerTask.java:28) [reactor-core-3.3.1.RELEASE.jar:3.3.1.RELEASE]
   	at java.util.concurrent.FutureTask.run(FutureTask.java:264) [?:?]
   	at java.util.concurrent.ScheduledThreadPoolExecutor$ScheduledFutureTask.run(ScheduledThreadPoolExecutor.java:304) [?:?]
   	at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1128) [?:?]
   	at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:628) [?:?]
   	at java.lang.Thread.run(Thread.java:830) [?:?]
   ```

   Note that `BenchmarkClient` hangs after this point and I could not manage to
   catch the exception anywhere.
