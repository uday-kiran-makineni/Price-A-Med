import React from "react";
import { useState } from "react";
import { AwesomeCaptcha } from "react-awesome-captcha";
import axios from "axios";

const UpdatePassword = () => {
  const [newData, setNewData] = useState({});
  const [isFocused, setIsFocused] = useState(false);
  const [alertMessage, setAlertMessage] = useState("");
  const [successMessage, setSuccessMessage] = useState("");
  const [isCaptchaValid, setIsCaptchaValid] = useState(false);
  const [section, setSection] = useState("section1");
  const [taskID, setTaskID] = useState("");
  const [isLoading, setIsLoading] = useState(false);

  const BACKEND_URL = import.meta.env.VITE_BACKEND_URL;

  const handleChange = (e) => {
    const { name, value } = e.target;
    setNewData({ ...newData, [name]: value });
  };

  const handleCaptchaValidation = (isValid) => {
    setIsCaptchaValid(isValid);
  };

  const ValidateAndSendOTP = async () => {
    const memberID = newData["memberID"] || "";
    const regex = /^ENM\$[A-Za-z0-9]{8}$/;

    if (!regex.test(memberID)) {
      setAlertMessage("Invalid Member ID");
      return;
    }

    setIsLoading(true);
    setAlertMessage("");

    try {
      const response = await axios.post(
        BACKEND_URL + "/auth/forgot-password/send-otp",
        { memberID: memberID }
      );

      if (response.data.success) {
        setTaskID(response.data.taskID);
        setSuccessMessage("OTP sent to " + response.data.email);
        setSection("section2");
      } else {
        setAlertMessage(response.data.message || "Failed to send OTP");
      }
    } catch (error) {
      console.log("Error:", error);
      if (error.response && error.response.data) {
        setAlertMessage(error.response.data.message || "Failed to send OTP");
      } else {
        setAlertMessage("Failed to send OTP. Please try again.");
      }
    } finally {
      setIsLoading(false);
    }
  };

  const ValidateAndSubmitOTP = async () => {
    const otp = newData["otp"] || "";

    if (otp.length < 6) {
      setAlertMessage("Invalid OTP. Must be at least 6 characters.");
      return;
    }

    setIsLoading(true);
    setAlertMessage("");

    try {
      const response = await axios.post(
        BACKEND_URL + "/auth/forgot-password/verify-otp",
        { taskID: taskID, otp: otp }
      );

      if (response.data.success) {
        setSuccessMessage("");
        setSection("section3");
      } else {
        setAlertMessage(response.data.message || "Invalid OTP");
      }
    } catch (error) {
      console.log("Error:", error);
      if (error.response && error.response.data) {
        setAlertMessage(error.response.data.message || "Invalid OTP");
      } else {
        setAlertMessage("Failed to verify OTP. Please try again.");
      }
    } finally {
      setIsLoading(false);
    }
  };

  const ValidateAndChangePassword = async () => {
    const password = newData["password"] || "";
    const cpassword = newData["cpassword"] || "";

    if (password === "" || cpassword === "") {
      setAlertMessage("All fields are required");
      return;
    }

    const regex =
      /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[!@#$%^&*])[A-Za-z\d!@#$%^&*]{8,}$/;

    if (!regex.test(password)) {
      setAlertMessage("Choose a strong password (min 8 chars, uppercase, lowercase, number, special char)");
      return;
    }

    if (password !== cpassword) {
      setAlertMessage("The passwords did not match");
      return;
    }

    if (!isCaptchaValid) {
      setAlertMessage("Invalid Captcha");
      return;
    }

    setIsLoading(true);
    setAlertMessage("");

    try {
      const response = await axios.post(
        BACKEND_URL + "/auth/forgot-password/reset-password",
        { 
          memberID: newData["memberID"],
          taskID: taskID, 
          password: password 
        }
      );

      if (response.data.success) {
        setSection("section4");
      } else {
        setAlertMessage(response.data.message || "Failed to reset password");
      }
    } catch (error) {
      console.log("Error:", error);
      if (error.response && error.response.data) {
        setAlertMessage(error.response.data.message || "Failed to reset password");
      } else {
        setAlertMessage("Failed to reset password. Please try again.");
      }
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="flex justify-center items-center">
      {section == "section1" && (
        <div>
          <div className="text-3xl mb-8">Forgot Password ?</div>
          <div className="font-bold h-6 mt-3 text-gray-5 00 text-md leading-8 uppercase">
            {""}
            MEMBER ID
          </div>
          <div className="bg-white my-2 p-1 flex border border-gray-500 rounded">
            <input
              type="text"
              onChange={handleChange}
              value={newData["memberID"] || ""}
              name="memberID"
              placeholder="Enter your Member ID"
              onFocus={() => {
                setIsFocused(true);
                setAlertMessage("");
              }}
              onBlur={() => setIsFocused(false)}
              className="p-3 px-5 pr-20 appearance-none outline-none w-full text-gray-800 "
            />
          </div>

          {!isFocused && alertMessage != "" && (
            <p className="text-sm text-center text-[red] mt-5">
              {alertMessage}
            </p>
          )}

          <button
            className="w-full h-[50px] bg-green-500 text-white rounded-md hover:bg-slate-600 font-semibold mt-5 disabled:bg-gray-400"
            onClick={ValidateAndSendOTP}
            disabled={isLoading}
          >
            {isLoading ? "Sending..." : "Send OTP"}
          </button>
        </div>
      )}

      {section === "section2" && (
        <div>
          <div className="text-3xl mb-8">OTP sent to associated email</div>
          {successMessage && (
            <p className="text-sm text-center text-green-600 mb-4">
              {successMessage}
            </p>
          )}
          <div className="font-bold h-6 mt-3 text-gray-5 00 text-md leading-8 uppercase">
            {""}
            OTP
          </div>
          <div className="bg-white my-2 p-1 flex border border-gray-500 rounded">
            <input
              type="text"
              onChange={handleChange}
              value={newData["otp"] || ""}
              name="otp"
              placeholder="Enter OTP"
              onFocus={() => {
                setIsFocused(true);
                setAlertMessage("");
              }}
              onBlur={() => setIsFocused(false)}
              className="p-3 px-5 pr-20 appearance-none outline-none w-full text-gray-800 "
            />
          </div>

          {!isFocused && alertMessage != "" && (
            <p className="text-sm text-center text-[red] mt-5">
              {alertMessage}
            </p>
          )}

          <button
            className="w-full h-[50px] bg-green-500 text-white rounded-md hover:bg-slate-600 font-semibold mt-5 disabled:bg-gray-400"
            onClick={ValidateAndSubmitOTP}
            disabled={isLoading}
          >
            {isLoading ? "Verifying..." : "Submit"}
          </button>
        </div>
      )}

      {section === "section3" && (
        <div>
          <div className="text-3xl mb-8">Change Password</div>
          <div className="font-bold h-6 mt-3 text-gray-5 00 text-md leading-8 uppercase">
            {""}
            Set Password
          </div>
          <div className="bg-white my-2 p-1 flex border border-gray-500 rounded">
            <input
              type="password"
              onChange={handleChange}
              value={newData["password"] || ""}
              name="password"
              placeholder="Enter a password"
              onFocus={() => {
                setIsFocused(true);
                setAlertMessage("");
              }}
              onBlur={() => setIsFocused(false)}
              className="p-3 px-5 pr-20 appearance-none outline-none w-full text-gray-800 "
            />
          </div>

          <div className="font-bold h-6 mt-3 text-gray-5 00 text-md leading-8 uppercase">
            {""}
            Confirm Password
          </div>
          <div className="bg-white my-2 p-1 flex border border-gray-500 rounded">
            <input
              type="password"
              onChange={handleChange}
              value={newData["cpassword"] || ""}
              name="cpassword"
              placeholder="Re-enter the password"
              onFocus={() => {
                setIsFocused(true);
                setAlertMessage("");
              }}
              onBlur={() => setIsFocused(false)}
              className="p-3 px-5 pr-20 appearance-none outline-none w-full text-gray-800 "
            />
          </div>

          <div
            onFocus={() => {
              setIsFocused(true);
              setAlertMessage("");
            }}
            onBlur={() => setIsFocused(false)}
          >
            <AwesomeCaptcha
              onValidate={handleCaptchaValidation}
              className={"flex justify-between w-full font-medium"}
            />
          </div>

          {!isFocused && alertMessage != "" && (
            <p className="text-sm text-center text-[red] mt-5">
              {alertMessage}
            </p>
          )}

          <button
            className="w-full h-[50px] bg-green-500 text-white rounded-md hover:bg-slate-600 font-semibold mt-5 disabled:bg-gray-400"
            onClick={ValidateAndChangePassword}
            disabled={isLoading}
          >
            {isLoading ? "Resetting..." : "Reset Password"}
          </button>
        </div>
      )}

      {section === "section4" && (
        <div className="container md:mt-10">
          <div className="flex flex-col items-center">
            <div className="text-green-400">
              <svg
                className="w-24 h-24"
                fill="currentColor"
                viewBox="0 0 20 20"
                xmlns="https://www.w3.org/2000/svg"
              >
                <path
                  fillRule="evenodd"
                  d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z"
                  clipRule="evenodd"
                />
              </svg>
            </div>

            <div className="text-lg font-semibold text-gray-500">
              Password is changed successfully.
            </div>
            <a className="mt-10" href="/login">
              <button className="h-10 px-5 text-green-700 transition-colors duration-150 border border-gray-300 rounded-lg focus:shadow-outline hover:bg-green-500 hover:text-white">
                Login
              </button>
            </a>
            <a href="/" className="text-sm mt-2 underline">
              Back to Home
            </a>
          </div>
        </div>
      )}
    </div>
  );
};

export default UpdatePassword;
