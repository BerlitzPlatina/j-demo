POST /api/mq/direct ──────────────────────────► [queue.direct.1] ──► DirectQueueOneHandler
   (đi qua default exchange "", routing key = tên queue)

POST /api/mq/direct-exchange ──► (direct.mode)
                          ├── key = "direct.one" ──► [queue.direct.1] ──► DirectQueueOneHandler
                          └── key = "direct.two" ──► [queue.2]        ──► QueueTwoHandler

POST /api/mq/fanout ──► (fanout.mode) ─┬──────► [queue.direct.1] ──► DirectQueueOneHandler
                                       └──────► [queue.2]        ──► QueueTwoHandler

POST /api/mq/topic ───► (topic.mode)
                          ├── key khớp "queue.#" ──► (fanout.mode) ──► cả 2 queue trên
                          ├── key khớp "*.queue" ──► [queue.2]      ──► QueueTwoHandler
                          └── key khớp "3.queue" ──► [3.queue]      ──► QueueThreeHandler

POST /api/mq/headers ─► (headers.mode)   (bỏ qua routing key, match theo header)
                          ├── x-match=all: type=report VÀ format=pdf ──► [queue.headers.all] ──► HeadersQueueAllHandler
                          └── x-match=any: type=report HOẶC format=pdf ──► [queue.headers.any] ──► HeadersQueueAnyHandler

POST /api/mq/delay ───► (delay.mode, x-delayed-message) ──► [delay.queue] ──► DelayQueueHandler
   (cần plugin rabbitmq_delayed_message_exchange + rabbitmq.delay.enabled=true)

Mọi queue trên đều gắn dead letter exchange (dlx.mode) ──► [dlq.queue] ──► DeadLetterQueueHandler
