package vkcrawler.bfs.prototype3

trait ExchangeBackend {
  def init:Unit
  def publish(tag:String, msg: Any):Unit
}

trait DummyExchangeBackend extends ExchangeBackend {
  def init:Unit = {}
  def publish(tag:String, msg: Any):Unit = {}
}

trait RabbitMQExchangeBackend extends ExchangeBackend {
  def init:Unit = {
    throw new Exception("Not implemented")
  }

  def publish(tag:String, msg: Any):Unit = {
    throw new Exception("Not implemented")
  }
}