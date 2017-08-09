#include "ImageProcess.h"
#include <stdio.h>

#include "time.h"
#include "opencv2/opencv.hpp"

#include <fstream>
#include "opencv2/highgui/highgui.hpp"
#include "opencv2/stitching/detail/warpers.hpp"
#include "opencv2/stitching/detail/matchers.hpp"
#include "opencv2/stitching.hpp"

#include <android/log.h>

using namespace std;
using namespace cv;

ImageProcess::ImageProcess(){ 
}
ImageProcess::~ImageProcess(){ 
}

bool try_use_gpu = true;

bool ImageProcess::stitch(vector<Mat> paths, const char* outPath) {
    /*
     stitcher.setRegistrationResol(0.1);
     Ptr<detail::OrbFeaturesFinder> finder;
     finder = new detail::OrbFeaturesFinder();
     stitcher.setFeaturesFinder(finder);
     
     Ptr<detail::BestOf2NearestMatcher> matcher;
     matcher = new detail::BestOf2NearestMatcher(true, 0.3f, 6, 6);
     stitcher.setFeaturesMatcher(matcher);
     
     Ptr<detail::BundleAdjusterRay> ray;
     ray = new detail::BundleAdjusterRay();
     stitcher.setBundleAdjuster(ray);
     
     Ptr<detail::GainCompensator> compensator;
     compensator = new detail::GainCompensator();
     stitcher.setExposureCompensator(compensator);
     
     Ptr<cv::CylindricalWarper> warper;
     warper = new cv::CylindricalWarper();
     stitcher.setWarper(warper);
     
     Ptr<detail::Blender> blender;
     blender = new detail::MultiBandBlender();
     stitcher.setBlender(blender);
     */
    
    clock_t start;
    clock_t finish;
    double   duration;
    
    start = clock();
    
    Mat pano;
    Stitcher stitcher = Stitcher::createDefault(try_use_gpu);
    stitcher.setRegistrationResol(0.6);
    
//    stitcher.estimateTransform(paths);
//    Stitcher::Status status = stitcher.composePanorama(pano);
    Stitcher::Status status = stitcher.stitch(paths, pano);
    finish = clock();
    __android_log_print(ANDROID_LOG_DEBUG,"myTag", "status === %d seconds\n", status);
    
    if (status != Stitcher::OK) {
        return false;
    } 
    
    imwrite("/sdcard/result.jpg", pano);
    bool ret = cropImage(pano, outPath);
    duration = (double)(finish - start) / CLOCKS_PER_SEC;
    __android_log_print(ANDROID_LOG_DEBUG,"myTag", "duration === %f seconds\n", duration);

    return ret;
}


void showImage(string nameOfWindow, Mat &img, int Height, int Width);
Rect findInsideRect(vector<Point> &contours_poly, Mat &contourMask);
bool checkInteriorExterior(const Mat&mask, const Rect&interiorBB,
                int&top, int&bottom, int&left, int&right);
bool sortX(Point a, Point b);
bool sortY(Point a, Point b);

bool ImageProcess::cropImage(Mat img_ori, const char * outPath){
	int Height = 1200; // Scale images to suitable size
	int Width = 800;

//	Mat img_ori = imread(inPath, CV_LOAD_IMAGE_COLOR);

#ifdef VERBOSE
	showImage("Original image", img_ori, Height, Width);
#endif

	Mat img;
	cvtColor(img_ori, img, CV_BGR2GRAY);

#ifdef VERBOSE
	showImage("Gray image", img, Height, Width);
#endif

	threshold(img, img, 0, 255, THRESH_BINARY);

#ifdef VERBOSE
	showImage("Binarized gray image", img, Height, Width);
#endif

	vector<vector<Point> > contours;
	vector<Vec4i> hierarchy;
	int largest_contour_index = 0;
	int largest_area = 0;
	findContours(img.clone(), contours, hierarchy, CV_RETR_EXTERNAL,
			CV_CHAIN_APPROX_SIMPLE); // CV_CHAIN_APPROX_NONE
	for (int i = 0; i < contours.size(); i++)
	{
		double a = contourArea(contours[i], false); //  Find the area of contour
		if (a > largest_area)
		{
			largest_area = a;
			largest_contour_index = i;      //Store the index of largest contour
		}
	}

	/// Draw filled contour to obtain a mask with interior parts
	Mat contourMask = Mat::zeros(img.size(), CV_8UC1);
	drawContours(contourMask, contours, largest_contour_index, Scalar(255), CV_FILLED, 8,
			hierarchy, 0, Point());

#ifdef VERBOSE
	cout << "contours.size(): " << contours.size() << endl;
	cout << "largest_area: " << largest_area << endl;
	cout << "largest_contour_index: " << largest_contour_index << endl;
	cout << "contours[largest_contour_index].size(): "
			<< contours[largest_contour_index].size() << endl;
	showImage("Filled contour image", contourMask, Height, Width);
#endif

/*
	/// Approximate contours to polygons + get bounding rects and circles
	// vector<Point> contours_poly;
	vector<vector<Point> > contours_poly(1);
	Rect outerBoundRect;
	approxPolyDP(Mat(contours[largest_contour_index]), contours_poly[0], 10, true);
	outerBoundRect = boundingRect(Mat(contours_poly[0]));

	Mat im_poly = Mat::zeros(img.size(), CV_8UC1);
	drawContours(im_poly, contours_poly, 0, Scalar(255), CV_FILLED, 8, hierarchy, 0, Point());

#ifdef VERBOSE
	/// Draw polygonal contour + bonding rects

	// add rectangle to the image
	rectangle(im_poly, outerBoundRect.tl(), outerBoundRect.br(),
			Scalar(0, 255, 0), 10, 8, 0);
	cout << "contours_poly[0].size(): " << contours_poly[0].size() << endl;
	showImage("Polygon image with outer bounding box", im_poly, Height, Width);
#endif
*/

	// find the rectangle inside a polygon
	Rect innerBoundRect = findInsideRect(contours[largest_contour_index], contourMask);
	Mat croppedImage = img_ori(innerBoundRect);
	bool res = imwrite(outPath, croppedImage);


#ifdef VERBOSE
	Mat mask2 = Mat::zeros(img.rows, img.cols, CV_8UC1);
	rectangle(mask2, innerBoundRect, Scalar(255), -1);
	Mat maskedImage;
	img_ori.copyTo(maskedImage);
	for (unsigned int y = 0; y < maskedImage.rows; ++y)
		for (unsigned int x = 0; x < maskedImage.cols; ++x)
		{
			maskedImage.at<Vec3b>(y, x)[2] = 255;
		}
	img_ori.copyTo(maskedImage, mask2);
	showImage("Masked image", maskedImage, Height, Width);

	showImage("Cropped image", croppedImage, Height, Width);

#endif
        return res;
}

bool ImageProcess::cropImage(const char * inPath, const char * outPath){
    Mat img_ori = imread(inPath, CV_LOAD_IMAGE_COLOR);
    cropImage(img_ori, outPath);
}

void showImage(string nameOfWindow, Mat &img, int Height, int Width)
{
	namedWindow(nameOfWindow, WINDOW_NORMAL); // Create a window for display.
	resizeWindow(nameOfWindow, Height, Width);
	imshow(nameOfWindow, img);
	while(1)
	{
		if('a' == waitKey())
			break;
	}
}

Rect findInsideRect(vector<Point> &contours_poly, Mat &contourMask)
{
	// sort contour in x/y directions to easily find min/max and next
	vector<Point> cSortedX = contours_poly;
	sort(cSortedX.begin(), cSortedX.end(), sortX);

	vector<Point> cSortedY = contours_poly;
	sort(cSortedY.begin(), cSortedY.end(), sortY);

	unsigned int minXId = 0;
	unsigned int maxXId = cSortedX.size() - 1;

	unsigned int minYId = 0;
	unsigned int maxYId = cSortedY.size() - 1;

	Rect interiorBB;

	while ((minXId < maxXId) && (minYId < maxYId))
	{
		Point min(cSortedX[minXId].x, cSortedY[minYId].y);
		Point max(cSortedX[maxXId].x, cSortedY[maxYId].y);

		interiorBB = Rect(min.x, min.y, max.x - min.x, max.y - min.y);

		// out-codes: if one of them is set, the rectangle size has to be reduced at that border
		int ocTop = 0;
		int ocBottom = 0;
		int ocLeft = 0;
		int ocRight = 0;

		bool finished = checkInteriorExterior(contourMask, interiorBB, ocTop,
				ocBottom, ocLeft, ocRight);
		if (finished)
		{
			break;
		}

		// reduce rectangle at border if necessary
		if (ocLeft)
			++minXId;
		if (ocRight)
			--maxXId;

		if (ocTop)
			++minYId;
		if (ocBottom)
			--maxYId;
	}
	return interiorBB;
}


bool checkInteriorExterior(const Mat&mask, const Rect&interiorBB,
		int&top, int&bottom, int&left, int&right)
{
// return true if the rectangle is fine as it is!
	bool returnVal = true;

	Mat sub = mask(interiorBB);

	unsigned int x = 0;
	unsigned int y = 0;

// count how many exterior pixels are at the
	unsigned int cTop = 0; // top row
	unsigned int cBottom = 0; // bottom row
	unsigned int cLeft = 0; // left column
	unsigned int cRight = 0; // right column
// and choose that side for reduction where mose exterior pixels occured (that's the heuristic)

	for (y = 0, x = 0; x < sub.cols; ++x)
	{
		// if there is an exterior part in the interior we have to move the top side of the rect a bit to the bottom
		if (sub.at<unsigned char>(y, x) == 0)
		{
			returnVal = false;
			++cTop;
		}
	}

	for (y = sub.rows - 1, x = 0; x < sub.cols; ++x)
	{
		// if there is an exterior part in the interior we have to move the bottom side of the rect a bit to the top
		if (sub.at<unsigned char>(y, x) == 0)
		{
			returnVal = false;
			++cBottom;
		}
	}

	for (y = 0, x = 0; y < sub.rows; ++y)
	{
		// if there is an exterior part in the interior
		if (sub.at<unsigned char>(y, x) == 0)
		{
			returnVal = false;
			++cLeft;
		}
	}

	for (x = sub.cols - 1, y = 0; y < sub.rows; ++y)
	{
		// if there is an exterior part in the interior
		if (sub.at<unsigned char>(y, x) == 0)
		{
			returnVal = false;
			++cRight;
		}
	}

// that part is ugly and maybe not correct, didn't check whether all possible combinations are handled. Check that one please. The idea is to set `top = 1` iff it's better to reduce the rect at the top than anywhere else.
	if (cTop > cBottom)
	{
		if (cTop > cLeft)
			if (cTop > cRight)
				top = 1;
	}
	else if (cBottom > cLeft)
		if (cBottom > cRight)
			bottom = 1;

	if (cLeft >= cRight)
	{
		if (cLeft >= cBottom)
			if (cLeft >= cTop)
				left = 1;
	}
	else if (cRight >= cTop)
		if (cRight >= cBottom)
			right = 1;

	return returnVal;
}

bool sortX(Point a, Point b)
{
	bool ret = false;
	if (a.x == a.x)
		if (b.x == b.x)
			ret = a.x < b.x;

	return ret;
}

bool sortY(Point a, Point b)
{
	bool ret = false;
	if (a.y == a.y)
		if (b.y == b.y)
			ret = a.y < b.y;

	return ret;
}
