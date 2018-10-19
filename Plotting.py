import numpy as np
import matplotlib.pyplot as plt 
import matplotlib.patches as patches
from matplotlib import style 
import seaborn as sns
import pandas as pd
import csv

style.use('ggplot')
#sensors_1536745135536
xaxis,x, y, z, x2, y2, z2, latitude, longitude, speed, anomaly = np.loadtxt('Data4\sensors_1539148085752.csv', delimiter = ';',unpack=True)

threshcheck = 5.6
threshdiff = 5.5
threshstd = 2
DetectedLat = []
DetectedLong = []

def graph():
    i=0
    plt.figure(1)
    plt.ylim(-9.8, 9.8)
    
    
    
    plt.plot(xaxis,z, 'b',label='Transformed Z-axis Linear Acceleration')

    plt.legend(loc = 2)    
    plt.ylabel('m/s2')
    plt.xlabel('timestamps')

    

    #while i < len(xaxis):
        #plt.axvline(xaxis[i],color= 'g', linestyle= 'dashed')
        #i= i+200
    #plt.axvline(xaxis[len(xaxis)-1],color= 'g', linestyle= 'dashed')

    plt.title('Linear Acceleration Data Collected with Road Surface Data Collection Application')
    #plt.show()

def GraphLocations():
    plt.figure(2)
    plt.ylabel('Longitude')
    plt.xlabel('Latitude')
    plt.title('Location of Road Anomalies')

    i=0
    plt.scatter(-33.958141, 18.464571,color='b')
    plt.scatter(-33.958080, 18.464573,color='b')
    plt.scatter(-33.957740, 18.464612,color='b')
    plt.scatter( -33.956755, 18.464706,color='b')
    plt.scatter(-33.955637, 18.464686,color='b')
    plt.scatter(-33.955687, 18.464627,color='r')
    

    while i < len(DetectedLat):
        if(DetectedLat[i]!=0 and i !=6 ):
            plt.scatter(DetectedLat[i],DetectedLong[i],color='r')
            print(DetectedLat[i], " ",DetectedLong[i])
        i= i+1
    plt.show()

def checkthresh():
    detected1=0
    i=0
    z2 = []
    sec=0
    found =0
    graph() #Graphing Function
    while i < len(z):   
        if(abs(z[i])>=threshcheck): #Performs check if Acceleration above Threshold
            z2.append(z[i]) 
            print(z[i])
            plt.scatter(xaxis[i],z[i],color='r')
            detected1 = detected1 + 1
            if sec == 0: #Method to Plot a 1 second window around detected anomaly
                found = 1
                DetectedLat.append(latitude[i])
                DetectedLong.append(longitude[i])
                plt.axvline(xaxis[i],color= 'g', linestyle= 'dashed')
            sec=sec+1
        else:
            z2.append(None)
            if found == 1:
                if sec == 200:
                    plt.axvline(xaxis[i],color= 'g', linestyle= 'dashed')
                    sec = 0
                    found = 0 
                else: 
                    sec=sec+1    
        i=i+1
    plt.plot(xaxis,z2,color='r')    # Plots the parts of the grpah where the acceleration exceeds threshold.
    plt.axhline(y=threshcheck,color= 'r', linestyle= 'dashed')  # Provides the threshold level on the graph.
    plt.axhline(y=-threshcheck,color= 'r', linestyle= 'dashed')
    plt.show()
    GraphLocations()


def diffcheck():
    detected2=0
    k=0
    c=0
    z3 = []
    sec=0
    found =0
    graph() #Graphing Function

    while k < len(z)-1:
        diff = abs(z[k]-z[k+1]) # Computes the Difference between consecutive readings.
        if(diff>=threshdiff): #Performs check if difference above Difference Threshold.
            detected2 = detected2+1
            if sec == 0: #Method to Plot a 1 second window around detected anomaly
                DetectedLat.append(latitude[k])
                DetectedLong.append(longitude[k])
                found = 1
                plt.axvline(xaxis[k],color= 'g', linestyle= 'dashed')
            sec=sec+1
            if z[k] not in z3:
                z3.append(z[k])
            if z[k+1] not in z3:
                z3.append(z[k+1]) # Appending the readings if the difference exceeds Difference Threshold.
        elif z[k] not in z3:
            z3.append(None)
            if found == 1:
                if sec == 200:
                    plt.axvline(xaxis[k],color= 'g', linestyle= 'dashed')
                    sec = 0
                    found = 0 
                else: 
                    sec=sec+1 
        k += 1
        
    while len(z3) != len(xaxis):
        z3.append(None)     

    plt.plot(xaxis,z3,color='r') #Plots the readings that exceeds difference threshold in red. 
    plt.show()
    GraphLocations()



def stdDevcheck():    
    j=0
    detected3=0
    z4 =[]
    o=0
    graph()
    while j < len(xaxis):
        b = z[j:j+40].std() #Calculate the standard deviation for the specified window size.
        #plt.axvline(xaxis[j],color= 'r', linestyle= 'dashed') #Plotting the window barriers.
        if(abs(b)>=threshstd): #Check if standard deviation for the window exceeds the threshold.
            z4.extend(z[j:j+40]) #Add the window to a separate array.
            detected3 = detected3 + 1
            DetectedLat.append(latitude[j])
            DetectedLong.append(longitude[j])
            #print(b)
        else:
            o=0
            while o < 40 and len(z4)<len(xaxis):
                z4.append(None)
                o = o+1
        j=j+40
        
    print(detected3)
    plt.plot(xaxis,z4,color='r') #Plot the detected windows in red.
    plt.show()
    GraphLocations()


def main():
    a = int(input("Select which Algorthm to run:\n      1. Threshold Checking\n      2. Difference Checking\n      3. Standard Deviation Checking\n"))
    if a==1:
        checkthresh()
    elif a==2:
        diffcheck()
    elif a==3:
        stdDevcheck()
main()