#include <WorkerTask.h>

#include <Poco/AutoPtr.h>
#include <Poco/Util/Application.h>
#include <Poco/NumberFormatter.h>

#include <Poco/Net/HTTPClientSession.h>
#include <Poco/Net/HTTPRequest.h>
#include <Poco/Net/HTTPResponse.h>
#include <Poco/Net/HTTPMessage.h>

#include <TaskNotifications.h>

#include <Poco/JSON/Parser.h>
#include <PluginsCache.h>

#include <Poco/StreamCopier.h>

#ifdef PERFORMANCE_COUNT
#include <atomic>
#include <chrono>
static std::atomic<int>			process_perf_time = 0;
static std::atomic<int>         process_perf_count = 0;

static std::atomic<int>			post_perf_time = 0;
static std::atomic<int>         post_perf_count = 0;

static std::atomic<int>			post_size = 0;
#endif

WorkerTask::WorkerTask(Poco::NotificationQueue& queue, int n):
	Task("WorkerTask " + Poco::NumberFormatter::format(n)),
	m_jobQueue(queue)
{
}

void WorkerTask::runTask()
{
	auto& app = Poco::Util::Application::instance();

	const std::string serverHost = app.config().getString("server.host");
	const Poco::UInt16 serverPort = app.config().getUInt("server.port");

	poco_information(app.logger(), name() + " started");

	Poco::AutoPtr<Poco::Notification> pNot(m_jobQueue.waitDequeueNotification());
	while (true)
	{
		StopNotification* sjob = dynamic_cast<StopNotification*> (pNot.get());

		if (sjob != nullptr)
			break;

		CrawlJobNotification* cjob = dynamic_cast<CrawlJobNotification*> (pNot.get());

		//got new job
		Poco::JSON::Parser parser;

		try
		{
			auto v = parser.parse(cjob->get());

			Poco::DynamicAny result = parser.result();

			auto obj = result.extract<Poco::JSON::Object::Ptr>();

			auto type = obj->get("type").extract<std::string>();

			auto& pc = PluginsCache::instance();

			if (!pc.get(type)->isValid(obj))
				throw Poco::RuntimeException("Task is not valid for plugin " + pc.get(type)->getName());

#ifdef PERFORMANCE_COUNT
			auto process_start = std::chrono::system_clock::now();
#endif
			auto ans = pc.get(type)->process(obj);
#ifdef PERFORMANCE_COUNT
			auto process_stop = std::chrono::system_clock::now();
			process_perf_time += std::chrono::duration_cast<std::chrono::milliseconds>(process_stop - process_start).count();
			process_perf_count++;
			if (process_perf_count != 0 && process_perf_count % 10 == 0)
				poco_information_f3(app.logger().get("perf"), "PROCESS: total: %d ms count: %d mean: %.2f ms", 
					process_perf_time.load(), 
					process_perf_count.load(),
					(double)process_perf_time.load() / process_perf_count.load()
				);

#endif

			//send back

			using namespace Poco::Net;
#ifdef PERFORMANCE_COUNT
			auto post_start = std::chrono::system_clock::now();
#endif
			HTTPClientSession session(serverHost, serverPort);

			HTTPRequest req(HTTPRequest::HTTP_POST, "/postTask", HTTPMessage::HTTP_1_1);

			//query parameters
			//req.add("", "");
			std::stringstream ss;
			Poco::JSON::Stringifier::stringify(ans, ss);
			//Poco::JSON::Stringifier::stringify(ans, std::cout);

			req.setContentLength(ss.str().length());
			req.setContentType("application/json");
			auto& reqStream = session.sendRequest(req);
			std::string postData = ss.str();
			reqStream << postData;

			HTTPResponse resp;
			auto& respStream = session.receiveResponse(resp);

			std::string postAns;
			Poco::StreamCopier::copyToString(respStream, postAns);
#ifdef PERFORMANCE_COUNT
			auto post_stop = std::chrono::system_clock::now();
			post_perf_time += std::chrono::duration_cast<std::chrono::milliseconds>(post_stop - post_start).count();
			post_perf_count++;
			post_size += postData.size();
			if (post_perf_count != 0 && post_perf_count % 10 == 0) {
				poco_information_f3(app.logger().get("perf"), "POST: total: %d ms count: %d mean: %.2f ms",
					post_perf_time.load(),
					post_perf_count.load(),
					(double)post_perf_time.load() / post_perf_count.load()
					);

				poco_information_f1(app.logger().get("perf"), "POST traffic: %.2f bytes", (double)post_size.load() / post_perf_count.load());
			}
#endif
			if (resp.getStatus() == HTTPResponse::HTTP_OK)
				poco_information(app.logger(), "POST task: " + postAns);
			else
				poco_warning(app.logger(), "POST task: " + postAns);
		}	
		catch (Poco::Exception& e)
		{
			poco_error(app.logger(), "Error process task: " + e.displayText());
		}

		pNot = m_jobQueue.waitDequeueNotification();
	}

	poco_information(app.logger(), name() + " finished");
}