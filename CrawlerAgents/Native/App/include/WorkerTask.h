#ifndef Crawler_WorkerTask_INCLUDED
#define Crawler_WorkerTask_INCLUDED

#include <Poco/Task.h>
#include <Poco/NotificationQueue.h>

class WorkerTask: public Poco::Task
{
	Poco::NotificationQueue& m_jobQueue;
public:
	WorkerTask(Poco::NotificationQueue& queue, int n);

	void runTask();
protected:
	//virtual void process(std::string const& URL) = 0;
};

#endif //Crawler_WorkerTask_INCLUDED