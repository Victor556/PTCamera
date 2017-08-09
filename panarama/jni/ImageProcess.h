#ifndef JNI_TEST_IMAGE_PROCESS
#define JNI_TEST_IMAGE_PROCESS
#include "opencv2/opencv.hpp"

using namespace std;
using namespace cv;

class ImageProcess{
public:
    ImageProcess();
    ~ImageProcess();
    bool stitch(vector<Mat>, const char * outPath);
    bool cropImage(Mat inImage, const char * outPath);
    bool cropImage(const char * inPath, const char * outPath);
};
#endif
