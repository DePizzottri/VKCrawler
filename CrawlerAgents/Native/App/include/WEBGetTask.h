#include <Poco/Task.h>
#include <Poco/NotificationQueue.h>

using namespace Poco;
using namespace std;

class WEBGetTask : public Poco::Task {
	NotificationQueue& m_jobQueue;
public:
	WEBGetTask(NotificationQueue & queue);

	void runTask();
};
